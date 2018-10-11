package net.namekdev.quakemonkey.utils;

import static org.junit.Assert.assertEquals;

import java.util.function.BiConsumer;

import org.junit.Test;

import net.namekdev.quakemonkey.utils.BiConsumerMultiplexer;

public class BiConsumerMultiplexerTest {

	private int i = 0, j = 0;

	@Test
	public void testMultiplexer() {
		BiConsumerMultiplexer<Integer, Integer> mult = new BiConsumerMultiplexer<>();

		// Basic stuff
		assertEquals(0, mult.size());

		// One consumer
		BiConsumer<Integer, Integer> cons1 = new BiConsumer<Integer, Integer>() {
			@Override
			public void accept(Integer t, Integer u) {
				i = u * t;
			}
		};
		mult.addBiConsumer(cons1);

		mult.dispatch(3, 4);
		assertEquals(12, i);
		assertEquals(1, mult.size());

		// Add a consumer twice
		mult.addBiConsumer(cons1);
		assertEquals(1, mult.size());

		// Multiple Consumers
		mult.addBiConsumer(new BiConsumer<Integer, Integer>() {
			@Override
			public void accept(Integer t, Integer u) {
				j = t / u;
			}
		});

		mult.dispatch(9, 3);
		assertEquals(27, i);
		assertEquals(3, j);
		assertEquals(2, mult.size());

		// Remove & Clear
		mult.removeBiConsumer(cons1);
		assertEquals(1, mult.size());

		mult.clear();
		assertEquals(0, mult.size());

		// Numbers unchanged
		mult.dispatch(9, 3);
		assertEquals(27, i);
		assertEquals(3, j);

	}

}
