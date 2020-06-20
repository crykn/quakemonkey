package net.quakemonkey.messages;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Serializes a delta message efficiently.
 * 
 * @author Ben Ruijl
 */
public class DiffMessageSerializer extends Serializer<DiffMessage> {

	@Override
	public DiffMessage read(Kryo kryo, Input input,
			Class<? extends DiffMessage> type) {
		short messageID = input.readShort();
		int flagSize = input.readVarInt(true);

		byte[] flags = new byte[flagSize];
		input.readBytes(flags, 0, flagSize);

		int intCount = 0;
		for (int i = 0; i < 8 * flagSize; i++) {
			if ((flags[i / 8] & (1 << (i % 8))) != 0) {
				intCount++;
			}
		}

		return DiffMessage.POOL.obtain().set(messageID, flags,
				input.readInts(intCount));
	}

	@Override
	public void write(Kryo kryo, Output output, DiffMessage diff) {
		output.writeShort(diff.getMessageId());
		output.writeVarInt(diff.getFlags().length, true);
		output.write(diff.getFlags());

		output.writeInts(diff.getData(), 0, diff.getData().length);

		// output.setPosition(output.position() + diff.getData().length * 4);
	}

}
