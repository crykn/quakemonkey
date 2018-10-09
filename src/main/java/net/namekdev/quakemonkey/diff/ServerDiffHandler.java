package net.namekdev.quakemonkey.diff;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.google.common.base.Preconditions;

import net.namekdev.quakemonkey.diff.messages.AckMessage;
import net.namekdev.quakemonkey.diff.messages.DiffMessage;
import net.namekdev.quakemonkey.diff.messages.LabeledMessage;
import net.namekdev.quakemonkey.diff.utils.BufferPool;

/**
 * Handles the dispatching of messages of type {@code T} to clients, using a
 * protocol of delta messages.
 * <p>
 * Important: make sure that you call
 * {@link DiffClassRegistration#registerClasses()} before starting the server.
 * 
 * @author Ben Ruijl
 * 
 * @param <T>
 *            Message type
 * @see #ClientDiffHandler
 */
public class ServerDiffHandler<T> implements Listener {
	protected static final Logger LOG = Logger
			.getLogger(ServerDiffHandler.class.getName());
	private final Server server;
	private final short numHistory;
	private final Map<Connection, DiffConnectionHandler<T>> diffConnections;
	private final boolean alwaysSendDiffs;

	public ServerDiffHandler(Server server, short numHistory,
			boolean alwaysSendDiffs) {
		Preconditions.checkNotNull(server);
		Preconditions.checkArgument(numHistory >= 1);

		this.server = server;
		this.numHistory = numHistory;
		this.alwaysSendDiffs = alwaysSendDiffs;
		diffConnections = new HashMap<Connection, DiffConnectionHandler<T>>();

		server.addListener(this);
	}

	public ServerDiffHandler(Server server, short numHistory) {
		this(server, numHistory, false);
	}

	public ServerDiffHandler(Server server, boolean alwaysSendDiffs) {
		this(server, (short) 20, alwaysSendDiffs);
	}

	public ServerDiffHandler(Server server) {
		this(server, false);
	}

	public void dispatchMessageToAll(T msg) {
		for (Connection connection : server.getConnections()) {
			dispatchMessageToConnection(connection, msg);
		}
	}

	/**
	 * Dispatches a message to all clients in the filter.
	 */
	public void dispatchMessageToConnections(Collection<Connection> recipients,
			T msg) {
		for (Connection connection : server.getConnections()) {
			if (recipients.contains(connection)) { // FIXME Reference
													// comparison (?)
				dispatchMessageToConnection(connection, msg);
			}
		}
	}

	private void dispatchMessageToConnection(Connection connection, T msg) {
		if (!diffConnections.containsKey(connection)) {
			diffConnections.put(connection, new DiffConnectionHandler<T>(
					server.getKryo(), numHistory, alwaysSendDiffs));
		}

		DiffConnectionHandler<T> diffConnection = diffConnections
				.get(connection);
		LabeledMessage newMessage = diffConnection.generateSnapshot(msg);
		server.sendToUDP(connection.getID(), newMessage);

		// Everything back to pools
		if (newMessage.getPayloadMessage() instanceof DiffMessage) {
			DiffMessage diffMessage = (DiffMessage) newMessage
					.getPayloadMessage();

			BufferPool.DEFAULT.freeByteArray(diffMessage.getFlag());
			BufferPool.DEFAULT.freeIntArray(diffMessage.getData());
			DiffMessage.POOL.free(diffMessage);
		}
		LabeledMessage.POOL.free(newMessage);
	}

	/**
	 * Returns the lag in terms of how many messages sent to the client haven't
	 * been acknowledged. If the connection does not exist, for example because
	 * no messages have been sent yet, 0 is returned.
	 * 
	 * @param conn
	 *            Connection to client
	 * @return Connection lag
	 */
	public int getLag(Connection conn) {
		if (!diffConnections.containsKey(conn)) {
			throw new IllegalStateException(
					"Trying to get lag of connection that does not exist (yet).");
		}

		return diffConnections.get(conn).getLag();
	}

	@Override
	public void disconnected(Connection connection) {
		diffConnections.remove(connection);
	}

	@Override
	public void received(Connection con, Object m) {
		if (m instanceof AckMessage && diffConnections.containsKey(con)) {
			DiffConnectionHandler<T> diffConnection = diffConnections.get(con);
			diffConnection.registerAck(((AckMessage) m).getId());
		}
	}
}
