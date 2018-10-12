package net.quakemonkey.utils;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import net.quakemonkey.utils.pool.BufferPool;

public class Utils {

	private Utils() {
		// not used
	}

	/**
	 * Packs a message into a buffer.
	 * 
	 * @param message
	 *            The actual message.
	 * @param kryoSerializer
	 *            The kryo instance used to serialize the message.
	 * @return A buffer containing the serialized message.
	 */
	public static ByteBuffer messageToBuffer(Object message,
			/* @Nullable ByteBuffer target, */ Kryo kryoSerializer) {
		/*
		 * ByteBuffer buffer = target == null ?
		 * BufferPool.DEFAULT.obtainByteBuffer(Short.MAX_VALUE) : target;
		 */
		ByteBuffer buffer = BufferPool.DEFAULT
				.obtainByteBuffer(Short.MAX_VALUE);

		Output output = new Output(buffer.array());

		// output.setPosition(2);
		kryoSerializer.writeClassAndObject(output, message);
		buffer.position(output.position());
		buffer.flip();
		// short dataLength = (short) (buffer.remaining() - 2);
		// buffer.putShort(dataLength);
		// buffer.position(0);

		return buffer;
	}

	/**
	 * @param x
	 * @return whether <code>x</code> is a power of two.
	 */
	public static boolean isPowerOfTwo(int x) {
		return (x & (x - 1)) == 0;
	}

	/**
	 * Converts a short value to a cyclic index for an array with the size
	 * <code>mod</code>.
	 * <p>
	 * The cyclic property requires powers of two as a size so the edge cases
	 * (i.e. <code>Short.MAX_VALUE + 1</code>) are handled properly.
	 * 
	 * @param mod
	 *            The array size. Only powers of <code>2</code> are of use here.
	 * @param val
	 *            The current value that should get converted to the cyclic
	 *            index.
	 * @return The cyclic index.
	 */
	public static int getIndexForPos(int mod, short val) {
		if (val < 0) {
			return Math.abs(Short.MIN_VALUE - val) % mod;
		} else {
			return val % mod;
		}
	}
}
