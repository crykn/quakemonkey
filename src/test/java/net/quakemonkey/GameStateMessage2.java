package net.quakemonkey;

import java.util.ArrayList;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class GameStateMessage2 {
	private List<Integer> position;
	private List<Integer> orientation;

	public GameStateMessage2() {
		// default public constructor
	}

	public GameStateMessage2(List<Integer> position,
			List<Integer> orientation) {
		this.position = position;
		this.orientation = orientation;
	}

	public List<Integer> getPosition() {
		return position;
	}

	public List<Integer> getOrientation() {
		return orientation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		GameStateMessage2 other = (GameStateMessage2) obj;
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
		return "GameStateMessage2 { pos: " + position + ", orientation: "
				+ orientation + "}";
	}

	public static class GameState2Serializer
			extends Serializer<GameStateMessage2> {
		
		private final boolean useCompression;
		
		public GameState2Serializer(boolean useCompression) {
			this.useCompression = useCompression;
		}
		
		public GameState2Serializer() {
			this(true);
		}
		
		@Override
		public GameStateMessage2 read(Kryo kryo, Input input,
				Class<? extends GameStateMessage2> cls) {
			List<Integer> position = new ArrayList<Integer>();
			List<Integer> orientation = new ArrayList<Integer>();
			input.setVariableLengthEncoding(useCompression);
			
			int n = input.readShort();
			for (int i = 0; i < n; ++i) {
				position.add(input.readInt(true));
			}

			n = input.readShort();
			for (int i = 0; i < n; ++i) {
				orientation.add(input.readInt(true));
			}

			return new GameStateMessage2(position, orientation);
		}

		@Override
		public void write(Kryo kryo, Output output, GameStateMessage2 object) {
			output.setVariableLengthEncoding(useCompression);
			
			int n = object.position.size();
			
			output.writeShort(n);
			for (int i = 0; i < n; ++i) {
				output.writeInt(object.position.get(i), true);
			}

			n = object.orientation.size();
			output.writeShort(n);
			for (int i = 0; i < n; ++i) {
				output.writeInt(object.orientation.get(i), true);
			}
		}
	}
}
