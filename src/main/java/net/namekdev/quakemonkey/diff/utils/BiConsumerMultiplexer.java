package net.namekdev.quakemonkey.diff.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import com.esotericsoftware.kryonet.Connection;

public class BiConsumerMultiplexer<T> {
	private List<BiConsumer<Connection, T>> listeners = new ArrayList<>();

	public void addBiConsumer(BiConsumer<Connection, T> listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeBiConsumer(BiConsumer<Connection, T> listener) {
		listeners.remove(listener);
	}

	public void dispatch(Connection source, T message) {
		for (BiConsumer<Connection, T> listener : listeners) {
			listener.accept(source, message);
		}
	}
}
