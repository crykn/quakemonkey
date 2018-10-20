package net.quakemonkey;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;

import net.quakemonkey.ClientDiffHandler;
import net.quakemonkey.DiffClassRegistration;
import net.quakemonkey.DiffConnectionHandler;
import net.quakemonkey.messages.DiffMessage;
import net.quakemonkey.messages.PayloadPackage;

public class SerializationTests {

	private Client createTestClient() {
		Client fakeClient = new FakeClient();
		Kryo kryoSerializer = fakeClient.getKryo();
		DiffClassRegistration.registerClasses(kryoSerializer);
		kryoSerializer.register(GameStateMessage.class,
				new GameStateMessage.GameStateSerializer());
		kryoSerializer.register(GameStateMessage2.class,
				new GameStateMessage2.GameState2Serializer());

		return fakeClient;
	}

	/**
	 * Scenario: server sends first gamestate and client receives it correctly.
	 * No DiffMessage should be sent here.
	 */
	@Test
	public void firstMessageReceiveTest() {
		Client client = createTestClient();

		// First server side
		DiffConnectionHandler<GameStateMessage> diffConnection = new DiffConnectionHandler<>(
				client.getKryo(), (short) 4);
		List<Float> position = Arrays.asList(new Float[] { 0.5f, 0.6f, 0.7f });
		List<Float> orientation = Arrays.asList(new Float[] { 0f, 0f, 1f });
		byte id = (byte) 1;
		final GameStateMessage message = new GameStateMessage("nmk", position,
				orientation, id);

		PayloadPackage messageToSend = diffConnection.generateSnapshot(message);

		// Now client side
		ClientDiffHandler<GameStateMessage> clientDiffHandler = new ClientDiffHandler<>(
				client, GameStateMessage.class, (short) 4);

		clientDiffHandler
				.addListener(new BiConsumer<Connection, GameStateMessage>() {
					@Override
					public void accept(Connection arg0, GameStateMessage msg) {
						assertEquals(msg, message);
					}
				});

		// will call listener above
		clientDiffHandler.processPackage(client, messageToSend);
	}

	/**
	 * Scenario: server sends two gamestates and client acknowledges only first,
	 * then the third one is being sent and now client receives it. Test if diff
	 * makes all correct.
	 * 
	 * @throws TimeoutException
	 */
	@Test
	public void snapshotDiffTest() throws TimeoutException {
		final Client client = createTestClient();

		DiffConnectionHandler<GameStateMessage> diffConnection = new DiffConnectionHandler<>(
				client.getKryo(), (short) 16, true);
		ClientDiffHandler<GameStateMessage> clientDiffHandler = new ClientDiffHandler<>(
				client, GameStateMessage.class, (short) 16);
		// ServerDiffHandler<GameStateMessage> serverDiffHandler = new
		// ServerDiffHandler<SerializationTests.GameStateMessage>(new
		// FakeServer());

		// Server sends first gamestate (Orientation: 0 0 1)
		String name = "nmk";
		List<Float> position = Arrays.asList(new Float[] { 0.5f, 0.6f, 0.7f });
		List<Float> orientation = Arrays.asList(new Float[] { 0f, 0f, 1f });
		byte id = (byte) 1;
		GameStateMessage message = new GameStateMessage(name, position,
				orientation, id);
		// should be: serverDiffHandler.dispatchMessage(fakeServer,
		// fakeServer.getConnections(), message);
		// but we'll do that directly
		PayloadPackage firstMessage = (PayloadPackage) diffConnection
				.generateSnapshot(message);

		// Client acknowledges first message.
		// should be: fakeClient.sendUDP(new
		// AckMessage(firstMessage.getLabel()));
		clientDiffHandler.processPackage(client, firstMessage);
		diffConnection.registerAck(firstMessage.getId());

		// Server sends second gamestate (Orientation: 1 0 1)
		position = new ArrayList<>(position); // clone
		orientation = new ArrayList<>(orientation); // clone
		orientation.set(0, 1f);
		message = new GameStateMessage("" + name, position, orientation,
				(byte) 2);
		/* final Object secondMessage = */ diffConnection
				.generateSnapshot(message);

		// Now Client didn't receive or didn't acknowledge properly 2nd
		// gamestate.
		// Server doesn't know what happened, so it should send delta based on
		// 1st and 3rd gamestate.

		// Server sends third gamestate (Orientation: 1 1 1)
		position = new ArrayList<>(position); // clone
		orientation = new ArrayList<>(orientation); // clone
		orientation.set(1, 1f);
		message = new GameStateMessage("" + name, position, orientation,
				(byte) 3);
		final PayloadPackage thirdMessage = diffConnection
				.generateSnapshot(message);

		final GameStateMessage testMessage = message;

		// Client receives snapshot delta based on 1st and 3rd gamestate.
		PayloadPackage messageReceived = thirdMessage;

		clientDiffHandler
				.addListener(new BiConsumer<Connection, GameStateMessage>() {
					@Override
					public void accept(Connection con, GameStateMessage msg) {
						assertEquals(msg.getPosition(),
								testMessage.getPosition());
						assertEquals(msg.getOrientation(),
								testMessage.getOrientation());
						assertEquals(msg.getName(), msg.getName());
					}
				});

		// will call listener above
		clientDiffHandler.processPackage(client, messageReceived);
	}

	@Test
	public void testTooLargeAck() {
		Client client = createTestClient();
		DiffConnectionHandler<GameStateMessage2> severDiffConnection = new DiffConnectionHandler<>(
				client.getKryo(), (short) 4);

		int previousLag = severDiffConnection.getLag();
		severDiffConnection.registerAck((short) -5);

		// Should not have changed, as the id was too low
		assertEquals(previousLag, severDiffConnection.getLag());
	}

	@Test
	public void testDifferentArrayLengths() {
		// Setup everything
		Client client = createTestClient();
		DiffConnectionHandler<GameStateMessage> severDiffConnection = new DiffConnectionHandler<>(
				client.getKryo(), (short) 4);
		ClientDiffHandler<GameStateMessage> clientDiffHandler = new ClientDiffHandler<>(
				client, GameStateMessage.class, (short) 16);

		// First state; simulate send, receive & ack
		List<Float> position = Arrays.asList(new Float[] { 1f, 1f, 1f, 1f });
		List<Float> orientation = Arrays.asList(new Float[] { 1f, 1f, 1f, 1f });
		final GameStateMessage gameStateMessage = new GameStateMessage(
				"asdfghjkl", position, orientation, (byte) 1);

		PayloadPackage firstPackage = severDiffConnection
				.generateSnapshot(gameStateMessage);

		clientDiffHandler.processPackage(client, firstPackage);
		severDiffConnection.registerAck(firstPackage.getId());

		// Second state; check if this one is received properly
		position = Arrays.asList(new Float[] { 2f, 2f });
		orientation = Arrays.asList(new Float[] { 2f, 2f });
		final GameStateMessage secondStateMessage = new GameStateMessage(
				"asdfghjkl", position, orientation, (byte) 1);

		PayloadPackage secondPackage = severDiffConnection
				.generateSnapshot(secondStateMessage);

		assertEquals(DiffMessage.class,
				secondPackage.getPayloadMessage().getClass());

		clientDiffHandler
				.addListener(new BiConsumer<Connection, GameStateMessage>() {
					@Override
					public void accept(Connection arg0, GameStateMessage msg) {
						assertEquals(secondStateMessage, msg);
					}
				});
		clientDiffHandler.processPackage(client, secondPackage);
	}

	/**
	 * The diff message is one bit larger than the original message -> the
	 * original message is sent instead.
	 */
	@Test
	public void testLargerDiffThanMessage() {
		// Setup everything
		Client client = createTestClient();
		DiffConnectionHandler<GameStateMessage2> severDiffConnection = new DiffConnectionHandler<GameStateMessage2>(
				client.getKryo(), (short) 4);

		// First state; simulate send & ack
		List<Integer> position = Arrays.asList(new Integer[] { 1, 1, 1, 1, 1 });
		List<Integer> orientation = Arrays
				.asList(new Integer[] { 1, 1, 1, 1, 1 });
		final GameStateMessage2 gameStateMessage = new GameStateMessage2(
				position, orientation);

		PayloadPackage firstPackage = severDiffConnection
				.generateSnapshot(gameStateMessage);

		assertEquals(GameStateMessage2.class,
				firstPackage.getPayloadMessage().getClass());

		severDiffConnection.registerAck(firstPackage.getId());

		// Second state; diff is larger than the original message -> package
		// holds the original message
		position = Arrays.asList(
				new Integer[] { 2, 2, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5 });
		orientation = Arrays.asList(new Integer[] { 2, 2, 2, 2, 2, 2, 2 });
		final GameStateMessage2 secondStateMessage = new GameStateMessage2(
				position, orientation);

		PayloadPackage secondPackage = severDiffConnection
				.generateSnapshot(secondStateMessage);

		assertEquals(GameStateMessage2.class,
				secondPackage.getPayloadMessage().getClass());
	}

	/**
	 * The server receives no ack until the snapshotHistoryCount was exceeded.
	 */
	@Test
	public void testTooLongNoAck() {
		// Setup everything
		Client client = createTestClient();
		DiffConnectionHandler<GameStateMessage2> severDiffConnection = new DiffConnectionHandler<GameStateMessage2>(
				client.getKryo(), (short) 4);

		// State
		List<Integer> position = Arrays.asList(new Integer[] { 1, 1, 1 });
		List<Integer> orientation = Arrays.asList(new Integer[] { 1, 1, 1 });
		final GameStateMessage2 gameStateMessage = new GameStateMessage2(
				position, orientation);

		// First package; ack
		PayloadPackage firstPckg = severDiffConnection
				.generateSnapshot(gameStateMessage);
		severDiffConnection.registerAck(firstPckg.getId());

		// Packages 2-5
		for (int i = 0; i < 4; i++) {
			// Simulate message send, but no ack
			severDiffConnection.generateSnapshot(gameStateMessage);
		}

		// Sending a complete new message, because the snapshot count was
		// exceeded
		assertEquals(GameStateMessage2.class,
				severDiffConnection.generateSnapshot(gameStateMessage)
						.getPayloadMessage().getClass());
	}

}
