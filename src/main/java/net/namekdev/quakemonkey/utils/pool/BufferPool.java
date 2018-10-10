package net.namekdev.quakemonkey.utils.pool;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * A pool for byte and integer arrays as well as buffers.
 * <p>
 * Beware that the obtained buffers and arrays may still contain data!
 */
public class BufferPool {
	public static final BufferPool DEFAULT = new BufferPool();

	private final DuplicatedKeysTreeMap<Integer, byte[]> byteArrayPool = new DuplicatedKeysTreeMap<Integer, byte[]>();
	private final DuplicatedKeysTreeMap<Integer, int[]> intArrayPool = new DuplicatedKeysTreeMap<Integer, int[]>();
	private final DuplicatedKeysTreeMap<Integer, ByteBuffer> byteBufferPool = new DuplicatedKeysTreeMap<Integer, ByteBuffer>(
			false);
	private final DuplicatedKeysTreeMap<Integer, IntBuffer> intBufferPool = new DuplicatedKeysTreeMap<Integer, IntBuffer>(
			false);

	public byte[] obtainByteArray(int minimumSize) {
		return obtainByteArray(minimumSize, false);
	}

	public byte[] obtainByteArray(int size, boolean exactSize) {
		synchronized (byteArrayPool) {
			byte[] array = exactSize ? byteArrayPool.poll(size)
					: byteArrayPool.pollCeiling(size);

			if (array == null) {
				array = new byte[size];
			}

			assert (array != null);
			return array;
		}
	}

	public void freeByteArray(byte[] array) {
		if (array == null)
			return;

		synchronized (byteArrayPool) {
			byteArrayPool.put(array.length, array);
		}
	}

	public int[] obtainIntArray(int size) {
		return obtainIntArray(size, false);
	}

	public int[] obtainIntArray(int size, boolean exactSize) {
		synchronized (intArrayPool) {
			int[] array = exactSize ? intArrayPool.poll(size)
					: intArrayPool.pollCeiling(size);

			if (array == null) {
				array = new int[size];
			}

			assert (array != null);
			return array;
		}
	}

	public void freeIntArray(int[] array) {
		if (array == null)
			return;

		synchronized (intArrayPool) {
			intArrayPool.put(array.length, array);
		}
	}

	public ByteBuffer obtainByteBuffer(int minimumSize) {
		return obtainByteBuffer(minimumSize, false);
	}

	public ByteBuffer obtainByteBuffer(int size, boolean exactSize) {
		synchronized (byteBufferPool) {
			ByteBuffer buffer = exactSize ? byteBufferPool.poll(size)
					: byteBufferPool.pollCeiling(size);

			if (buffer == null) {
				buffer = ByteBuffer.allocate(size);
			}

			assert (buffer != null);
			return buffer;
		}
	}

	public void freeByteBuffer(ByteBuffer buffer) {
		if (buffer == null)
			return;
		synchronized (byteBufferPool) {
			buffer.clear();
			byteBufferPool.put(buffer.capacity(), buffer);
		}
	}

	public IntBuffer obtainIntBuffer(int minimumSize) {
		return obtainIntBuffer(minimumSize, false);
	}

	public IntBuffer obtainIntBuffer(int size, boolean exactSize) {
		synchronized (intBufferPool) {
			IntBuffer buffer = exactSize ? intBufferPool.poll(size)
					: intBufferPool.pollCeiling(size);

			if (buffer == null) {
				buffer = IntBuffer.allocate(size);
			}

			assert (buffer != null);
			return buffer;
		}
	}

	public void freeIntBuffer(IntBuffer buffer) {
		if (buffer == null)
			return;

		synchronized (intBufferPool) {
			buffer.clear();
			intBufferPool.put(buffer.capacity(), buffer);
		}
	}
}
