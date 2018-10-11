package net.namekdev.quakemonkey;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import net.namekdev.quakemonkey.messages.AckMessage;
import net.namekdev.quakemonkey.messages.DiffMessage;
import net.namekdev.quakemonkey.messages.PayloadPackage;
import net.namekdev.quakemonkey.utils.BiConsumerMultiplexer;
import net.namekdev.quakemonkey.utils.Utils;
import net.namekdev.quakemonkey.utils.pool.BufferPool;

/**
 * Handles the client-side job of receiving either messages of type {@code T} or
 * delta messages. If a delta message is received, it is merged with a cached
 * old message. When the message is processed, an acknowledgment is sent to the
 * server.
 * <p>
 * Client can register message listeners for type {@code T} by calling
 * {@link #addListener()}. It is very important that the client does not listen
 * to message type {@code T} via other listeners.
 * <p>
 * Important: make sure that you call
 * {@link DiffClassRegistration#registerClasses()} before starting the client.
 * 
 * @author Ben Ruijl
 * 
 * @param <T>
 *            Message type
 */
public class ClientDiffHandler<T> {
	protected static final Logger LOG = Logger
			.getLogger(ClientDiffHandler.class.getName());
	private final Kryo kryoSerializer;
	private final Class<T> cls;
	private final ByteBuffer[] snapshots;
	private final BiConsumerMultiplexer<Connection, T> listeners;
	/**
	 * Position in cyclic array.
	 * 
	 * @see Utils#getIndexForPos(int, short)
	 */
	private short curPos;

	public ClientDiffHandler(Client client, Class<T> cls,
			short snapshotHistoryCount) {
		Preconditions.checkNotNull(client);
		Preconditions.checkNotNull(cls);
		Preconditions.checkArgument(snapshotHistoryCount >= 4);
		Preconditions.checkArgument(Utils.isPowerOfTwo(snapshotHistoryCount),
				"The snapshotHistoryCount has to be a power of two");

		this.kryoSerializer = client.getKryo();
		this.cls = cls;

		listeners = new BiConsumerMultiplexer<>();
		snapshots = new ByteBuffer[snapshotHistoryCount];

		client.addListener(new Listener() { // don't use a TypeListener for
											// performance reasons
			@Override
			public void received(Connection connection, Object object) {
				if (object instanceof PayloadPackage)
					processPackage(connection, (PayloadPackage) object);
			}
		});
	}

	public void addListener(BiConsumer<Connection, T> listener) {
		listeners.addBiConsumer(listener);
	}

	public void removeListener(BiConsumer<Connection, T> listener) {
		listeners.removeBiConsumer(listener);
	}

	/**
	 * Applies the delta message to the old message to generate a new message of
	 * type {@code T}.
	 * 
	 * @param oldMessage
	 *            The old message
	 * @param diffMessage
	 *            The delta message
	 * @return A new message of type <code>T</code> as a ByteBuffer.
	 */
	private ByteBuffer mergeMessage(ByteBuffer oldMessage,
			DiffMessage diffMessage) {
		byte[] diffFlags = diffMessage.getFlags();
		int[] diffData = diffMessage.getData();
		int dataIndex = 0;

		// Copy old message
		ByteBuffer newBuffer = BufferPool.DEFAULT.obtainByteBuffer(
				Math.max(oldMessage.remaining(), 8 * diffFlags.length * 4));
		newBuffer.put(oldMessage);
		newBuffer.position(0);

		for (int i = 0; i < 8 * diffFlags.length; i++) {
			if ((diffFlags[i / 8] & (1 << (i % 8))) != 0) {
				newBuffer.putInt(i * 4, diffData[dataIndex]);
				dataIndex++;
			}
		}

		return newBuffer;
	}

	/**
	 * Processes the arrival of either a message of type {@code T} or a delta
	 * message. Sends an acknowledgment to the server.
	 */
	@SuppressWarnings("unchecked")
	@VisibleForTesting
	void processPackage(Connection con, PayloadPackage msg) {
		short diff = (short) (msg.getId() - curPos);

		/* Message is too old; we already got a newer one */
		if (diff < 0) {
			if (LOG.isLoggable(Level.INFO)) {
				LOG.log(Level.INFO,
						"Discarding too old message; a newer one is already available: "
								+ msg.getId() + " vs. current " + curPos);
			}
			return;
		}
		// if (diff > snapshots.length)

		/* Message is up to date */
		int index = Utils.getIndexForPos(snapshots.length, msg.getId());

		if (cls.isInstance(msg.getPayloadMessage())) {
			/* > Received a full message */
			BufferPool.DEFAULT.freeByteBuffer(snapshots[index]);

			snapshots[index] = Utils.messageToBuffer(
					(T) msg.getPayloadMessage(), kryoSerializer);
		} else if (msg.getPayloadMessage() instanceof DiffMessage) {
			/* > Received a diff message */
			DiffMessage diffMessage = (DiffMessage) msg.getPayloadMessage();

			if (LOG.isLoggable(Level.FINE)) {
				LOG.log(Level.FINE,
						"Received diff message; it is based on snapshot "
								+ diffMessage.getMessageId());
			}

			int oldIndex = Utils.getIndexForPos(snapshots.length,
					diffMessage.getMessageId());
			ByteBuffer mergedMessage = mergeMessage(snapshots[oldIndex],
					diffMessage);

			BufferPool.DEFAULT.freeByteBuffer(snapshots[index]);
			snapshots[index] = mergedMessage;
		}

		/* Send an ACK back */
		con.sendUDP(AckMessage.POOL.obtain().set(msg.getId()));

		/* Broadcast received changes to listeners */
		curPos = msg.getId();

		Input input = new Input(snapshots[index].array());
		input.setPosition(2); // skip size

		listeners.dispatch(con, (T) kryoSerializer.readClassAndObject(input));
	}
}
