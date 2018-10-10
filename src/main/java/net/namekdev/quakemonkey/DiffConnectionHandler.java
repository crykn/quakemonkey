package net.namekdev.quakemonkey;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.esotericsoftware.kryo.Kryo;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import net.namekdev.quakemonkey.messages.DiffMessage;
import net.namekdev.quakemonkey.messages.LabeledMessage;
import net.namekdev.quakemonkey.utils.BufferUtils;
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
	protected static final Logger lOG = Logger
			.getLogger(DiffConnectionHandler.class.getName());
	private final Kryo kryoSerializer;
	private final ByteBuffer[] snapshots;
	private short curPos; // position in cyclic array
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
		Preconditions.checkArgument(snapshotHistoryCount >= 2);

		this.kryoSerializer = kryoSerializer;
		this.alwaysSendDiff = alwaysSendDiff;
		snapshots = new ByteBuffer[snapshotHistoryCount];

		curPos = 0;
		ackPos = -1;
	}

	public DiffConnectionHandler(Kryo kryoSerializer, short numSnapshots) {
		this(kryoSerializer, numSnapshots, false);
	}

	/**
	 * Adds a new message to the snapshot list and either returns the full
	 * message or a delta message if the latter is possible.
	 * 
	 * @param message
	 *            Message to add to snapshot list
	 * @return {@code message} or a delta message
	 */
	@VisibleForTesting
	LabeledMessage generateSnapshot(T message) {
		short oldPos = curPos;

		int index = oldPos % snapshots.length;
		BufferPool.DEFAULT.freeByteBuffer(snapshots[index]);
		ByteBuffer newMessage = snapshots[index] = BufferUtils
				.messageToBuffer(message, null, kryoSerializer);
		curPos++;

		// only allow positive positions
		if (curPos < 0) {
			curPos = 0;
		}

		if (ackPos < 0) {
			return LabeledMessage.POOL.obtain().set(oldPos, message);
		}

		/* Is the last received message too old? Send a full one */
		if (oldPos - ackPos > snapshots.length
				|| (ackPos - oldPos > Short.MAX_VALUE / 2 && Short.MAX_VALUE
						- ackPos + oldPos > snapshots.length)) {
			return LabeledMessage.POOL.obtain().set(oldPos, message);
		}

		ByteBuffer lastAckMessage = snapshots[ackPos % snapshots.length];

		Object delta = generateDelta(newMessage, lastAckMessage, ackPos);

		return LabeledMessage.POOL.obtain().set(oldPos,
				delta == null ? message : delta);
	}

	/**
	 * Gets the number of messages the server is lagging behind.
	 * 
	 * @return Number of messages left behind
	 */
	public int getLag() {
		if (curPos >= ackPos) {
			return curPos - ackPos;
		}

		return Short.MAX_VALUE - ackPos + curPos;
	}

	public void registerAck(short id) {
		// because the array is cyclic, the id could be in front of the old
		// ackPos, so we check if the difference between the two is very large (
		// > 4 minutes at 60 fps).
		if (id > ackPos || ackPos - id > Short.MAX_VALUE / 2) {
			if (lOG.isLoggable(Level.FINER)) {
				lOG.log(Level.FINER, "Client received message " + id);
			}
			ackPos = id;
			return;
		}

		if (lOG.isLoggable(Level.FINER)) {
			lOG.log(Level.FINER, "Client received old message " + id);
		}
	}

	/**
	 * Returns a delta message from <code>message</code> and
	 * <code>prevMessage</code> or just <code>message</code> if that happens to
	 * be smaller.
	 * 
	 * @param message
	 *            Message to send
	 * @param prevMessage
	 *            Previous message
	 * @return
	 * @see #alwaysSendDiff
	 */
	private Object generateDelta(ByteBuffer buffer, ByteBuffer previousBuffer,
			short prevID) {
		int intBound = (int) (Math.ceil(buffer.remaining() / 4)) * 4;
		previousBuffer.limit(intBound);
		buffer.limit(intBound); // set buffers to be the same size

		IntBuffer diffInts = BufferPool.DEFAULT.obtainIntBuffer(buffer.limit()); // great
		// overestimation

		// check block of size int
		int numBits = intBound / 4;
		int numBytes = (numBits - 1) / 8 + 1;
		byte[] flag = BufferPool.DEFAULT.obtainByteArray(numBytes);

		// also works if old and new are not the same size, but less efficiently
		int i = 0;
		while (buffer.remaining() >= 4) {
			int val = buffer.getInt();
			if (previousBuffer.remaining() < 4
					|| val != previousBuffer.getInt()) {
				diffInts.put(val);
				flag[i / 8] |= 1 << (i % 8);
			} else {
				flag[i / 8] &= ~(1 << (i % 8));
			}
			i++;
		}

		diffInts.flip();

		/* Check what is smaller, delta message or original buffer */
		Object retMessage = null;

		// TODO fix size calculation to be more accurate
		if (alwaysSendDiff || diffInts.remaining() * 4 + 8 < buffer.limit()) {
			int diffDataSize = diffInts.remaining();
			int[] diffData = BufferPool.DEFAULT.obtainIntArray(diffDataSize,
					true);

			diffInts.get(diffData, 0, diffDataSize);

			retMessage = DiffMessage.POOL.obtain().set(prevID, flag, diffData);
		} else {
			retMessage = null;
		}

		BufferPool.DEFAULT.freeIntBuffer(diffInts);

		return retMessage;
	}
}
