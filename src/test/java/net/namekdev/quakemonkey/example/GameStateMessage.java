package net.namekdev.quakemonkey.example;

import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Example master gamestate that has to be sent from the server to the client
 * every server tick.
 * 
 * @author Ben Ruijl
 * 
 */
public class GameStateMessage {
	private String name;
	private List<Float> position;
	private List<Float> orientation;
	private byte id;

	public GameStateMessage() {
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
		public void write(Kryo kryo, Output output, GameStateMessage object) {
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
