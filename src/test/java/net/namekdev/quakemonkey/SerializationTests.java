package net.namekdev.quakemonkey;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.junit.Test;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import net.namekdev.quakemonkey.messages.LabeledMessage;

public class SerializationTests {
	/**
	 * Scenario: server sends first gamestate and client receives it correctly.
	 * No DiffMessage should be sent here.
	 */
	@Test
	public void firstMessageReceiveTest() {
		Client fakeClient = new FakeClient();
		Kryo kryoSerializer = fakeClient.getKryo();
		DiffClassRegistration.registerClasses(kryoSerializer);
		kryoSerializer.register(GameStateMessage.class);

		// First server side
		DiffConnectionHandler<GameStateMessage> diffConnection = new DiffConnectionHandler<GameStateMessage>(
				kryoSerializer, (short) 16);
		List<Float> position = Arrays.asList(new Float[] { 0.5f, 0.6f, 0.7f });
		List<Float> orientation = Arrays.asList(new Float[] { 0f, 0f, 1f });
		byte id = (byte) 1;
		final GameStateMessage message = new GameStateMessage("nmk", position,
				orientation, id);

		LabeledMessage messageToSend = diffConnection.generateSnapshot(message);

		// Now client side
		ClientDiffHandler<GameStateMessage> clientDiffHandler = new ClientDiffHandler<GameStateMessage>(
				fakeClient, GameStateMessage.class, (short) 16);

		clientDiffHandler.addListener(
				new BiConsumer<Connection, SerializationTests.GameStateMessage>() {
					@Override
					public void accept(Connection arg0, GameStateMessage msg) {
						assertEquals(msg, message);
					}
				});

		// will call listener above
		clientDiffHandler.processMessage(fakeClient, messageToSend);
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
		final FakeClient fakeClient = new FakeClient();
		Kryo kryoSerializer = fakeClient.getKryo();
		DiffClassRegistration.registerClasses(kryoSerializer);
		kryoSerializer.register(GameStateMessage.class);

		DiffConnectionHandler<GameStateMessage> diffConnection = new DiffConnectionHandler<GameStateMessage>(
				kryoSerializer, (short) 20, true);
		ClientDiffHandler<GameStateMessage> clientDiffHandler = new ClientDiffHandler<GameStateMessage>(
				fakeClient, GameStateMessage.class, (short) 16);
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
		LabeledMessage firstMessage = (LabeledMessage) diffConnection
				.generateSnapshot(message);

		// Client acknowledges first message.
		// should be: fakeClient.sendUDP(new
		// AckMessage(firstMessage.getLabel()));
		clientDiffHandler.processMessage(fakeClient, firstMessage);
		diffConnection.registerAck(firstMessage.getLabel());

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
		final LabeledMessage thirdMessage = diffConnection
				.generateSnapshot(message);

		final GameStateMessage testMessage = message;

		// Client receives snapshot delta based on 1st and 3rd gamestate.
		LabeledMessage messageReceived = thirdMessage;

		clientDiffHandler.addListener(
				new BiConsumer<Connection, SerializationTests.GameStateMessage>() {
					@Override
					public void accept(Connection con, GameStateMessage msg) {
						assertEquals(msg.position, testMessage.position);
						assertEquals(msg.orientation, testMessage.orientation);
					}
				});
		// will call listener above
		clientDiffHandler.processMessage(fakeClient, messageReceived);
	}

	@DefaultSerializer(GameStateMessage.GameStateSerializer.class)
	private static class GameStateMessage {
		private String name;
		private List<Float> position;
		private List<Float> orientation;
		private byte id;

		public GameStateMessage() {
			// default public constructor
		}

		public GameStateMessage(String name, List<Float> position,
				List<Float> orientation, byte id) {
			this.name = name;
			this.position = position;
			this.orientation = orientation;
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public List<Float> getPosition() {
			return position;
		}

		public List<Float> getOrientation() {
			return orientation;
		}

		public byte getId() {
			return id;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof GameStateMessage)) {
				return false;
			}
			GameStateMessage message = (GameStateMessage) other;

			return name.equals(message.name)
					&& equalLists(position, message.position)
					&& equalLists(orientation, message.orientation)
					&& id == message.id;
		}

		public static <K> boolean equalLists(List<Float> one, List<Float> two) {
			if (one == null && two == null) {
				return true;
			}

			if ((one == null && two != null) || one != null && two == null
					|| one.size() != two.size()) {
				return false;
			}

			// to avoid messing the order of the lists we will use a copy
			one = new ArrayList<Float>(one);
			two = new ArrayList<Float>(two);

			Collections.sort(one);
			Collections.sort(two);
			return one.equals(two);
		}

		public static class GameStateSerializer
				extends Serializer<GameStateMessage> {
			@Override
			public GameStateMessage read(Kryo kryo, Input input,
					Class<? extends GameStateMessage> cls) {
				String name = input.readString();
				List<Float> position = new ArrayList<Float>();
				List<Float> orientation = new ArrayList<Float>();
				byte id;

				int n = input.readShort();
				for (int i = 0; i < n; ++i) {
					position.add(input.readFloat());
				}

				n = input.readShort();
				for (int i = 0; i < n; ++i) {
					orientation.add(input.readFloat());
				}

				id = input.readByte();

				return new GameStateMessage(name, position, orientation, id);
			}

			@Override
			public void write(Kryo kryo, Output output,
					GameStateMessage object) {
				output.writeString(object.name);

				int n = object.position.size();
				output.writeShort(n);
				for (int i = 0; i < n; ++i) {
					output.writeFloat(object.position.get(i));
				}

				n = object.orientation.size();
				output.writeShort(n);
				for (int i = 0; i < n; ++i) {
					output.writeFloat(object.orientation.get(i));
				}

				output.writeByte(object.id);
			}
		}
	}

	private static class FakeClient extends Client {
		@Override
		public int sendUDP(Object object) {
			// do nothing for a testing purposes.
			return 0;
		}

		@Override
		public void addListener(Listener listener) {
		}
	}
}
