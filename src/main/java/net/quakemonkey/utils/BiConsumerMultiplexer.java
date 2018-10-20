package net.quakemonkey.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.google.common.base.Preconditions;

public class BiConsumerMultiplexer<A, B> {
	private List<BiConsumer<A, B>> listeners = new ArrayList<>();

	/**
	 * Adds a {@link BiConsumer} to the multiplexer.
	 * 
	 * @param consumer
	 *            The consumer. May not be <code>null</code>.
	 */
	public void addBiConsumer(BiConsumer<A, B> consumer) {
		Preconditions.checkNotNull(consumer);

		if (!listeners.contains(consumer)) {
			listeners.add(consumer);
		}
	}

	public void removeBiConsumer(BiConsumer<A, B> consumer) {
		listeners.remove(consumer);
	}

	public int size() {
		return listeners.size();
	}

	public void clear() {
		listeners.clear();
	}

	/**
	 * Dispatches a message to the registered {@link BiConsumer}s.
	 * 
	 * @param a
	 * @param b
	 * @see BiConsumer#accept(Object, Object)
	 */
	public void dispatch(A a, B b) {
		for (BiConsumer<A, B> consumer : listeners) {
			consumer.accept(a, b);
		}
	}
}
