package net.namekdev.quakemonkey.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UtilsTest {

	@Test
	public void testPowers() {
		assertTrue(Utils.isPowerOfTwo(4));
		assertTrue(Utils.isPowerOfTwo(8));
		assertTrue(Utils.isPowerOfTwo(16));
		assertFalse(Utils.isPowerOfTwo(5));
		assertFalse(Utils.isPowerOfTwo(24));
	}

	@Test
	public void testPosToIndex() {
		// 4: -1,0,1, 2,4
		assertEquals(3, Utils.getIndexForPos(4, (short) -1));
		assertEquals(0, Utils.getIndexForPos(4, (short) 0));
		assertEquals(1, Utils.getIndexForPos(4, (short) 1));
		assertEquals(2, Utils.getIndexForPos(4, (short) 2));
		assertEquals(0, Utils.getIndexForPos(4, (short) 4));

		// 4: MAX, MIN, MIN+1
		assertEquals(3, Utils.getIndexForPos(4, Short.MAX_VALUE));
		assertEquals(0, Utils.getIndexForPos(4, Short.MIN_VALUE));
		assertEquals(1, Utils.getIndexForPos(4, (short) (Short.MIN_VALUE + 1)));

		// 8: MAX, MIN
		assertEquals(7, Utils.getIndexForPos(8, Short.MAX_VALUE));
		assertEquals(0, Utils.getIndexForPos(8, Short.MIN_VALUE));

		// 16: MIN+1
		assertEquals(1,
				Utils.getIndexForPos(16, (short) (Short.MIN_VALUE + 1)));
	}

}
