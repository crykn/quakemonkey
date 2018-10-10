package net.namekdev.quakemonkey;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import net.namekdev.quakemonkey.ClientDiffHandler;
import net.namekdev.quakemonkey.DiffClassRegistration;
import net.namekdev.quakemonkey.ServerDiffHandler;
import net.namekdev.quakemonkey.messages.LabeledMessage;

/**
 * An example server that shows how the snapshot network code works.
 * 
 * @author Ben Ruijl
 * 
 */
public class ServerTest {
	private int serverTicks = 0;
	private Server server;
	private ServerDiffHandler<GameStateMessage> diffHandler;
	private ClientDiffHandler<GameStateMessage> clientDiffHandler;

	private List<Integer> dropIndices = Arrays
			.asList(new Integer[] { 15, 23, 50, 51, 66 });
	private List<Integer> wrongSendIndices = Arrays
			.asList(new Integer[] { 45, 47 });
	private int maxIndex = 70;

	private Object lastReceivedObject, lastSentObject;

	private void setupServer() throws IOException {
		server = new Server();
		server.start();

		Kryo kryo = server.getKryo();

		DiffClassRegistration.registerClasses(kryo);
		kryo.register(GameStateMessage.class,
				new GameStateMessage.GameStateSerializer());

		diffHandler = new ServerDiffHandler<GameStateMessage>(server);

		server.bind(6143, 6143);

		server.addListener(new Listener() {
			@Override
			public void connected(Connection connection) {
				System.out.println("Connection closed from "
						+ connection.getRemoteAddressTCP());
			}

			@Override
			public void disconnected(Connection connection) {
				System.out.println("New connection from "
						+ connection.getRemoteAddressTCP());
			}
		});
	}

	private void setupClient() throws IOException {
		Client kryoClient = new Client();
		kryoClient.start();

		Kryo kryo = kryoClient.getKryo();
		DiffClassRegistration.registerClasses(kryo);
		kryo.register(GameStateMessage.class,
				new GameStateMessage.GameStateSerializer());

		kryoClient.connect(1000, "localhost", 6143, 6143);

		clientDiffHandler = new ClientDiffHandler<GameStateMessage>(kryoClient,
				GameStateMessage.class, (short) 30);
		clientDiffHandler
				.addListener(new BiConsumer<Connection, GameStateMessage>() {
					@Override
					public void accept(Connection con, GameStateMessage msg) {
						// Do something with the message
						System.out.println("Client received: '" + msg + "'");
						
						if (msg.getPosition().get(0) == maxIndex)
							lastReceivedObject = msg;
					}
				});
	}

	private void testDiff() {
		List<Float> orientation = Arrays
				.asList(new Float[] { 0.5f, 0.6f, 0.7f });

		while (true && serverTicks <= maxIndex) {
			Collection<Connection> connections = server.getConnections();
			if (connections.size() > 0) {
				/* Create message */
				List<Float> newPos = new ArrayList<Float>(
						Arrays.asList(new Float[] { (float) serverTicks,
								(float) (Float.MAX_VALUE / serverTicks),
								3.0f }));

				GameStateMessage newMessage = new GameStateMessage("test",
						newPos, orientation, (byte) 0);

				/* Dispatch message to client */
				if (!dropIndices.contains(serverTicks)) {
					System.out.println("Sent: '" + newMessage + "'");
					diffHandler.dispatchMessageToAll(newMessage);
				} else {
					System.out.println("Message " + serverTicks + " dropped");
				}

				// Send an old message
				if (wrongSendIndices.contains(serverTicks)) {
					server.sendToAllUDP(LabeledMessage.POOL.obtain()
							.set((short) (serverTicks - 20), newMessage));
					System.out.println(
							"Old message " + (serverTicks - 20) + " sent");
				}

				/* Check if the connection is lagging badly */
				for (Connection conn : server.getConnections()) {
					if (diffHandler.getLag(conn) > 20) {
						System.out
								.println("Client " + conn.getRemoteAddressUDP()
										+ " is lagging badly.");
					}
				}

				if (serverTicks == maxIndex)
					lastSentObject = newMessage;

				serverTicks++;
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		server.stop();

		assertEquals(lastSentObject, lastReceivedObject);
	}

	@Test
	public void test() throws IOException, InterruptedException {
		setupServer();
		setupClient();

		testDiff();
	}

}
