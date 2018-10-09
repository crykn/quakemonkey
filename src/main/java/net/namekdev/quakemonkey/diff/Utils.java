package net.namekdev.quakemonkey.diff;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import net.namekdev.quakemonkey.diff.utils.BufferPool;

class Utils {
	public static ByteBuffer messageToBuffer(Object message, ByteBuffer target,
			Kryo kryoSerializer) {
		// Could let the caller pass their own in
		ByteBuffer buffer = target == null
				? BufferPool.DEFAULT.obtainByteBuffer(Short.MAX_VALUE + 2)
				: target;

		Output output = new Output(buffer.array());

		output.setPosition(2);
		kryoSerializer.writeClassAndObject(output, message);
		buffer.position(output.position());
		buffer.flip();
		short dataLength = (short) (buffer.remaining() - 2);
		buffer.putShort(dataLength);
		buffer.position(0);

		return buffer;
	}
}
