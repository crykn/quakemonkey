package net.namekdev.quakemonkey;

import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class GameStateMessage {
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((orientation == null) ? 0 : orientation.hashCode());
		result = prime * result
				+ ((position == null) ? 0 : position.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GameStateMessage other = (GameStateMessage) obj;
		if (id != other.id)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (orientation == null) {
			if (other.orientation != null)
				return false;
		} else if (!orientation.equals(other.orientation))
			return false;
		if (position == null) {
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GameStateMessage { name: " + name + ", pos: " + position
				+ ", orientation: " + orientation + ", id: " + id + "}";
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
