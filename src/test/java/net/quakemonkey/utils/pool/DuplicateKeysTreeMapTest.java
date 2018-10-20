package net.quakemonkey.utils.pool;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.quakemonkey.utils.pool.DuplicatedKeysTreeMap;

public class DuplicateKeysTreeMapTest {

	@Test
	public void testPoll() {
		DuplicatedKeysTreeMap<Integer, Integer> map = new DuplicatedKeysTreeMap<Integer, Integer>();

		// Poll for empty bag
		assertEquals(null, map.poll(2));
		assertEquals(null, map.pollCeiling(2));

		// Poll for given bag
		map.put(1, 1);
		map.put(1, 5);
		map.put(1, 3);

		assertEquals(1, (int) map.poll(1));

		// Poll ceiling
		assertEquals(null, map.pollCeiling(4));

		map.put(3, 5);
		map.put(5, 4);
		map.put(5, 6);

		assertEquals(4, (int) map.pollCeiling(4));
	}

}
