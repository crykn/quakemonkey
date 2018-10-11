package net.namekdev.quakemonkey;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.esotericsoftware.kryo.Kryo;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import net.namekdev.quakemonkey.messages.DiffMessage;
import net.namekdev.quakemonkey.messages.PayloadPackage;
import net.namekdev.quakemonkey.utils.Utils;
import net.namekdev.quakemonkey.utils.pool.BufferPool;

/**
 * The server-side handler of generating delta messages for one connection. It
 * keeps track of a list of snapshots in a cyclic array and registers the last
 * snapshot that was successfully received by the client.
 * 
 * @author Ben Ruijl
 * @see #ServerDiffHandler
 * @param <T>
 *            Message type
 */
public class DiffConnectionHandler<T> {
	protected static final Logger LOG = Logger
			.getLogger(DiffConnectionHandler.class.getName());
	private final Kryo kryoSerializer;
	private final ByteBuffer[] snapshots;
	/**
	 * Position in cyclic array.
	 * 
	 * @see Utils#getIndexForPos(int, short)
	 */
	private short curPos;
	private short ackPos;

	/**
	 * If set to <code>false</code>, then the size of the full message and the
	 * message diff is compared; based on that the smaller message is being
	 * sent.
	 * 
	 * If set to <code>true</code>, then the message diff is always being sent
	 * instead of the full message.
	 */
	private final boolean alwaysSendDiff;

	public DiffConnectionHandler(Kryo kryoSerializer,
			short snapshotHistoryCount, boolean alwaysSendDiff) {
		Preconditions.checkNotNull(kryoSerializer);
		Preconditions.checkArgument(snapshotHistoryCount >= 4);
		Preconditions.checkArgument(Utils.isPowerOfTwo(snapshotHistoryCount),
				"The snapshotHistoryCount has to be a power of two");

		this.kryoSerializer = kryoSerializer;
		this.alwaysSendDiff = alwaysSendDiff;
		snapshots = new ByteBuffer[snapshotHistoryCount];

		curPos = 0;
		ackPos = (short) (-snapshotHistoryCount - 1); // needed, so the first
														// message is always
														// unacknowledged for
	}

	public DiffConnectionHandler(Kryo kryoSerializer, short numSnapshots) {
		this(kryoSerializer, numSnapshots, false);
	}

	/**
	 * Adds a new message to the snapshot list and either returns the full
	 * message or a {@linkplain #generateDelta(ByteBuffer, ByteBuffer, short)
	 * delta message} if the latter is possible and viable.
	 * 
	 * @param message
	 *            Message to add to snapshot list
	 * @return {@code message} or a delta message
	 */
	@VisibleForTesting
	PayloadPackage generateSnapshot(T message) {
		short oldPos = curPos;
		curPos++;

		int index = Utils.getIndexForPos(snapshots.length, oldPos);
		BufferPool.DEFAULT.freeByteBuffer(snapshots[index]);
		ByteBuffer newMessage = snapshots[index] = Utils
				.messageToBuffer(message, kryoSerializer);

		short diff = (short) (oldPos - ackPos);

		/* The last received message is too old; send a full one */
		if (diff < 0 || diff > snapshots.length) {
			if (LOG.isLoggable(Level.INFO)) {
				LOG.log(Level.INFO,
						"The last acknowledged message is too old; sending a full one");
			}
			return PayloadPackage.POOL.obtain().set(oldPos, message);
		}

		/* Send a normal diff message */
		ByteBuffer lastAckMessage = snapshots[Utils
				.getIndexForPos(snapshots.length, ackPos)];
		lastAckMessage.position(0); // the buffer could have been used before

		// Generate the delta message; is null if the message itself is smaller
		// (because of Kryo's serialization)
		Object delta = generateDelta(newMessage, lastAckMessage, ackPos);

		return PayloadPackage.POOL.obtain().set(oldPos,
				delta == null ? message : delta);
	}

	/**
	 * Gets the number of messages the server is lagging behind.
	 * 
	 * @return Number of messages left behind
	 */
	public int getLag() {
		return Math.abs((short) (curPos - ackPos));
	}

	public void registerAck(short id) {
		// because the ack-messages could arrive in the wrong order, we have to
		// check if the received ack-message is the latest one
		short diff = (short) (id - ackPos);

		if (diff > 0) {
			if (LOG.isLoggable(Level.FINER)) {
				LOG.log(Level.FINER, "Client acknowledged message " + id);
			}
			ackPos = id;
			return;
		}

		if (LOG.isLoggable(Level.FINER)) {
			LOG.log(Level.FINER, "Client acknowledged _old_ message " + id
					+ " vs. current " + ackPos);
		}
	}

	/**
	 * Returns a delta message from <code>message</code> and
	 * <code>prevMessage</code> or just <code>message</code> if that happens to
	 * be smaller.
	 * 
	 * @param buffer
	 *            Message to send as a ByteBuffer
	 * @param previousBuffer
	 *            The last acknowledged message as a ByteBuffer
	 * @param diffToId
	 *            The id of the previous message.
	 * @return
	 * @see #alwaysSendDiff
	 */
	private Object generateDelta(ByteBuffer buffer, ByteBuffer previousBuffer,
			short diffToId) {
		int intBound = (int) (Math.ceil(
				Math.max(buffer.remaining(), previousBuffer.remaining()) / 4f))
				* 4;
		previousBuffer.limit(intBound);
		buffer.limit(intBound); // set buffers to be the same size

		IntBuffer diffInts = BufferPool.DEFAULT.obtainIntBuffer(buffer.limit()); // great
																					// overestimation

		// check block of size int
		int numBits = intBound / 4;
		int numBytes = (numBits - 1) / 8 + 1;
		byte[] flags = BufferPool.DEFAULT.obtainByteArray(numBytes);

		// also works if old and new are not the same size, but less efficiently
		int i = 0;
		while (buffer.remaining() >= 4) {
			int val = buffer.getInt();
			if (previousBuffer.remaining() < 4
					|| val != previousBuffer.getInt()) {
				// diff detected
				diffInts.put(val);
				flags[i / 8] |= 1 << (i % 8);
			} else {
				// same content -> no diff
				flags[i / 8] &= ~(1 << (i % 8));
			}
			i++;
		}

		diffInts.flip();

		/* Check what is smaller, delta message or original buffer */
		Object retMessage = null;

		if (alwaysSendDiff || (diffInts.remaining() * 4
				+ diffInts.remaining() / 8 + 1) < buffer.limit()) {
			int diffDataSize = diffInts.remaining();
			int[] diffData = BufferPool.DEFAULT.obtainIntArray(diffDataSize,
					true);

			diffInts.get(diffData, 0, diffDataSize);

			retMessage = DiffMessage.POOL.obtain().set(diffToId, flags,
					diffData);
		} else {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.log(Level.FINE,
						"The state message is smaller than the diff.");
			}

			retMessage = null;
		}

		BufferPool.DEFAULT.freeIntBuffer(diffInts);

		return retMessage;
	}
}
