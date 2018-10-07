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
 * to message type {@code T} through other methods (for example directly from
 * the server).
 * <p>
 * * Important: make sure that you call
 * {@link DiffClassRegistration#registerClasses()} before starting the client.
 * 
 * @author Ben Ruijl
 * 
 * @param <T>
 *            Message type
 */
public class ClientDiffHandler<T> {
	protected static final Logger log = Logger
			.getLogger(ClientDiffHandler.class.getName());
	private final Kryo kryoSerializer;
	private final short numSnapshots;
	private final Class<T> cls;
	private final List<T> snapshots;
	private final BiConsumerMultiplexer<T> listeners;
	private short curPos;

	public ClientDiffHandler(Client client, Class<T> cls, short numSnapshots) {
		this.kryoSerializer = client.getKryo();
		this.numSnapshots = numSnapshots;
		this.cls = cls;

		listeners = new BiConsumerMultiplexer();
		snapshots = new ArrayList<T>(numSnapshots);

		for (int i = 0; i < numSnapshots; i++) {
			snapshots.add(null);
		}

		client.addListener(new Listener() { // don't use a TypeListener for
											// performance reasons
			@Override
			public void received(Connection connection, Object object) {
				if (object instanceof LabeledMessage)
					accept(connection, (LabeledMessage) object);
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
	 * @return A new message of type {@code T}
	 */
	public T mergeMessage(T oldMessage, DiffMessage diffMessage) {
		ByteBuffer oldBuffer = Utils.messageToBuffer(oldMessage, null,
				kryoSerializer);

		// Copy old message
		ByteBuffer newBuffer = BufferPool.Default.obtainByteBuffer(32767);
		newBuffer.put(oldBuffer);
		newBuffer.position(0);

		BufferPool.Default.saveByteBuffer(oldBuffer);

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

		BufferPool.Default.saveByteBuffer(newBuffer);

		return (T) obj;
	}

	/**
	 * Processes the arrival of either a message of type {@code T} or a delta
	 * message. Sends an acknowledgment to the server.
	 */
	public void accept(Connection con, LabeledMessage msg) {
		T payloadMessage = (T) msg.getMessage();

		boolean isNew = curPos == 0 || curPos < msg.getLabel()
				|| msg.getLabel() - curPos > Short.MAX_VALUE / 2;

		// message is too old
		if (curPos - msg.getLabel() > numSnapshots || (msg.getLabel()
				- curPos > Short.MAX_VALUE / 2
				&& Short.MAX_VALUE - msg.getLabel() + curPos > numSnapshots)) {

			if (log.isLoggable(Level.INFO)) {
				log.log(Level.INFO, "Discarding too old message: "
						+ msg.getLabel() + " vs. cur " + curPos);
			}
			return;
		}

		if (cls.isInstance(msg.getMessage())) { // received full message
			snapshots.set(msg.getLabel() % numSnapshots, payloadMessage);
		} else {
			if (msg.getMessage() instanceof DiffMessage) {
				if (log.isLoggable(Level.FINE)) {
					ByteBuffer logBuffer = Utils.messageToBuffer(payloadMessage,
							null, kryoSerializer);
					log.log(Level.FINE,
							"Received diff of size " + logBuffer.limit());
					BufferPool.Default.saveByteBuffer(logBuffer);
				}

				DiffMessage diffMessage = (DiffMessage) payloadMessage;

				T oldMessage = snapshots
						.get(diffMessage.getMessageId() % numSnapshots);
				T newMessage = mergeMessage(oldMessage, diffMessage);

				snapshots.set(msg.getLabel() % numSnapshots, newMessage);
			}
		}

		/* Send an ACK back */
		con.sendUDP(AckMessage.Pool.obtain().set(msg.getLabel()));

		/* Broadcast changes */
		if (isNew) {
			curPos = msg.getLabel();
			listeners.dispatch(con, snapshots.get(curPos % numSnapshots));
		} else {
			// notify if message was old, for testing
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Old message received: " + msg.getLabel()
						+ " vs. cur " + curPos);
			}
		}
	}
}
