package net.quakemonkey;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.google.common.base.Preconditions;

import net.quakemonkey.messages.AckMessage;
import net.quakemonkey.messages.DiffMessage;
import net.quakemonkey.messages.PayloadPackage;
import net.quakemonkey.utils.Utils;
import net.quakemonkey.utils.pool.BufferPool;

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
public class ServerDiffHandler<T> {
	protected static final Logger LOG = Logger
			.getLogger(ServerDiffHandler.class.getName());
	private final Server server;
	private final short snapshotHistoryCount;
	private final Map<Connection, DiffConnectionHandler<T>> diffConnections;
	private final boolean alwaysSendDiffs;

	/**
	 * @param server
	 *            The used server.
	 * @param snapshotHistoryCount
	 *            The count of snapshots to keep. Has to be above <code>4</code>
	 *            (otherwise it would be quite useless on bad connections) and a
	 *            power of two (because the handler uses a short as a cyclic
	 *            index).
	 * @param alwaysSendDiffs
	 *            Whether the server should always send diff messages or compare
	 *            their size to the original message first.
	 */
	public ServerDiffHandler(Server server, short snapshotHistoryCount,
			boolean alwaysSendDiffs) {
		Preconditions.checkNotNull(server);
		Preconditions.checkArgument(snapshotHistoryCount >= 4);
		Preconditions.checkArgument(Utils.isPowerOfTwo(snapshotHistoryCount),
				"The snapshotHistoryCount has to be a power of two");

		this.server = server;
		this.snapshotHistoryCount = snapshotHistoryCount;
		this.alwaysSendDiffs = alwaysSendDiffs;
		diffConnections = new HashMap<Connection, DiffConnectionHandler<T>>();

		server.addListener(new Listener() {
			@Override
			public void disconnected(Connection connection) {
				diffConnections.remove(connection);
			}

			@Override
			public void received(Connection con, Object m) {
				if (m instanceof AckMessage
						&& diffConnections.containsKey(con)) {
					DiffConnectionHandler<T> diffConnection = diffConnections
							.get(con);
					diffConnection.registerAck(((AckMessage) m).getId());
				}
			}
		});
	}

	public ServerDiffHandler(Server server, short snapshotHistoryCount) {
		this(server, snapshotHistoryCount, false);
	}

	public ServerDiffHandler(Server server, boolean alwaysSendDiffs) {
		this(server, (short) 32, alwaysSendDiffs);
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
					server.getKryo(), snapshotHistoryCount, alwaysSendDiffs));
		}

		DiffConnectionHandler<T> diffConnection = diffConnections
				.get(connection);
		PayloadPackage newMessage = diffConnection.generateSnapshot(msg);
		server.sendToUDP(connection.getID(), newMessage);

		// Everything back to pools
		if (newMessage.getPayloadMessage() instanceof DiffMessage) {
			DiffMessage diffMessage = (DiffMessage) newMessage
					.getPayloadMessage();

			BufferPool.DEFAULT.freeByteArray(diffMessage.getFlags());
			BufferPool.DEFAULT.freeIntArray(diffMessage.getData());
			DiffMessage.POOL.free(diffMessage);
		}
		PayloadPackage.POOL.free(newMessage);
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
		Preconditions.checkState(diffConnections.containsKey(conn),
				"Trying to get lag of a connection that does not exist (yet).");

		return diffConnections.get(conn).getLag();
	}

}
