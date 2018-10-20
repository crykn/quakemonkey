package net.quakemonkey.utils.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.quakemonkey.utils.pool.BufferPool;

public class BufferPoolTests {

	@Test
	public void testByteArrayPool() {
		// Obtain array
		byte[] b = BufferPool.DEFAULT.obtainByteArray(5);
		assertTrue(b.length >= 5);

		// Check if freed array is obtained again
		b[0] = 1;
		b[1] = 3;
		b[2] = 7;

		BufferPool.DEFAULT.freeByteArray(b);
		BufferPool.DEFAULT.freeByteArray(new byte[4]);

		byte[] c = BufferPool.DEFAULT.obtainByteArray(5);

		assertEquals(b, c);

		// Freeing null should do nothing
		BufferPool.DEFAULT.freeByteArray(null);

		// Obtain exact size
		assertNotEquals(c, BufferPool.DEFAULT.obtainByteArray(5, true));

		// Obtain a bigger one
		BufferPool.DEFAULT.freeByteArray(new byte[6]);
		assertEquals(6, BufferPool.DEFAULT.obtainByteArray(5).length);
	}

	@Test
	public void testIntArrayPool() {
		// Obtain array
		int[] b = BufferPool.DEFAULT.obtainIntArray(5);
		assertTrue(b.length >= 5);

		// Check if freed array is obtained again
		b[0] = 1;
		b[1] = 3;
		b[2] = 7;

		BufferPool.DEFAULT.freeIntArray(b);
		BufferPool.DEFAULT.freeIntArray(new int[4]);

		int[] c = BufferPool.DEFAULT.obtainIntArray(5);

		assertEquals(b, c);

		// Freeing null should do nothing
		BufferPool.DEFAULT.freeIntArray(null);

		// Obtain exact size
		assertNotEquals(c, BufferPool.DEFAULT.obtainIntArray(5, true));

		// Obtain a bigger one
		BufferPool.DEFAULT.freeIntArray(new int[6]);
		assertEquals(6, BufferPool.DEFAULT.obtainIntArray(5).length);
	}

	@Test
	public void testFreeIntBuffer() {
		// Freeing null should do nothing
		BufferPool.DEFAULT.freeIntBuffer(null);
	}

}
