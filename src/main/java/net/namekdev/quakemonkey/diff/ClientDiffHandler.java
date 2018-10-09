package net.namekdev.quakemonkey.diff;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
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

import net.namekdev.quakemonkey.diff.messages.AckMessage;
import net.namekdev.quakemonkey.diff.messages.DiffMessage;
import net.namekdev.quakemonkey.diff.messages.LabeledMessage;
import net.namekdev.quakemonkey.diff.utils.BiConsumerMultiplexer;
import net.namekdev.quakemonkey.diff.utils.BufferPool;

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
	private final short numSnapshots;
	private final Class<T> cls;
	private final List<T> snapshots;
	private final BiConsumerMultiplexer<Connection, T> listeners;
	private short curPos;

	public ClientDiffHandler(Client client, Class<T> cls, short numSnapshots) {
		Preconditions.checkNotNull(client);
		Preconditions.checkNotNull(cls);
		Preconditions.checkArgument(numSnapshots >= 1);

		this.kryoSerializer = client.getKryo();
		this.numSnapshots = numSnapshots;
		this.cls = cls;

		listeners = new BiConsumerMultiplexer<>();
		snapshots = new ArrayList<>(numSnapshots);

		for (int i = 0; i < numSnapshots; i++) {
			snapshots.add(null);
		}

		client.addListener(new Listener() { // don't use a TypeListener for
											// performance reasons
			@Override
			public void received(Connection connection, Object object) {
				if (object instanceof LabeledMessage)
					processMessage(connection, (LabeledMessage) object);
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
	 * @return A new message of type {@code T}. May be <code>null</code>.
	 */
	@SuppressWarnings("unchecked")
	private T mergeMessage(T oldMessage, DiffMessage diffMessage) {
		ByteBuffer oldBuffer = Utils.messageToBuffer(oldMessage, null,
				kryoSerializer);

		// Copy old message
		ByteBuffer newBuffer = BufferPool.DEFAULT
				.obtainByteBuffer(Short.MAX_VALUE);
		newBuffer.put(oldBuffer);
		newBuffer.position(0);

		BufferPool.DEFAULT.freeByteBuffer(oldBuffer);

		byte[] diffFlags = diffMessage.getFlag();
		int[] diffData = diffMessage.getData();
		int index = 0;

		for (int i = 0; i < 8 * diffFlags.length; i++) {
			if ((diffFlags[i / 8] & (1 << (i % 8))) != 0) {
				newBuffer.putInt(i * 4, diffData[index]);
				index++;
			}
		}

		Input input = new Input(newBuffer.array());
		input.setPosition(2); // skip size
		Object obj = kryoSerializer.readClassAndObject(input);

		BufferPool.DEFAULT.freeByteBuffer(newBuffer);

		return (T) obj;
	}

	/**
	 * Processes the arrival of either a message of type {@code T} or a delta
	 * message. Sends an acknowledgment to the server.
	 */
	@SuppressWarnings("unchecked")
	@VisibleForTesting
	void processMessage(Connection con, LabeledMessage msg) {
		/* Message is too old */
		if (msg.getLabel() > 0) {
			if (curPos - msg.getLabel() > numSnapshots
					|| (msg.getLabel() - curPos > Short.MAX_VALUE / 2
							&& Short.MAX_VALUE - msg.getLabel()
									+ curPos > numSnapshots)) {
				if (LOG.isLoggable(Level.INFO)) {
					LOG.log(Level.INFO, "Discarding too old message: "
							+ msg.getLabel() + " vs. cur " + curPos);
				}
				return;
			}
		}

		if (cls.isInstance(msg.getPayloadMessage())) {
			/* Received full message */
			snapshots.set(msg.getLabel() % numSnapshots,
					(T) msg.getPayloadMessage());
		} else if (msg.getPayloadMessage() instanceof DiffMessage) {
			/* Received diff message */
			if (LOG.isLoggable(Level.FINE)) {
				ByteBuffer logBuffer = Utils.messageToBuffer(
						msg.getPayloadMessage(), null, kryoSerializer);
				LOG.log(Level.FINE,
						"Received diff of size " + logBuffer.limit());
				BufferPool.DEFAULT.freeByteBuffer(logBuffer);
			}

			DiffMessage diffMessage = (DiffMessage) msg.getPayloadMessage();

			T oldMessage = snapshots
					.get(diffMessage.getMessageId() % numSnapshots);
			T newMessage = mergeMessage(oldMessage, diffMessage);

			snapshots.set(msg.getLabel() % numSnapshots, newMessage);
		}

		/* Send an ACK back */
		con.sendUDP(AckMessage.POOL.obtain().set(msg.getLabel()));

		/* Broadcast received changes to listeners */
		boolean isNew = curPos == 0 || curPos < msg.getLabel()
				|| msg.getLabel() - curPos > Short.MAX_VALUE / 2;

		if (isNew) {
			curPos = msg.getLabel();
			listeners.dispatch(con, snapshots.get(curPos % numSnapshots));
		} else {
			// log if message was old, for testing
			if (LOG.isLoggable(Level.FINE)) {
				LOG.log(Level.FINE, "Old message received: " + msg.getLabel()
						+ " vs. cur " + curPos);
			}
		}
	}
}
