package net.quakemonkey.utils.pool;

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;

/**
 * A tree map that can hold multiple values for the same key.
 *
 * @param <K>
 * @param <V>
 */
class DuplicatedKeysTreeMap<K, V> {
	private final TreeMap<K, Queue<V>> map = new TreeMap<K, Queue<V>>();
	private final Comparator<? super V> comparator;

	/**
	 * Determines whether bags for a specific key should get removed, when they
	 * hold no more elements. Default is <code>true</code>.
	 */
	public final boolean shouldRemoveSubBagsWhenEmpty;

	public DuplicatedKeysTreeMap() {
		this(true);
	}

	public DuplicatedKeysTreeMap(boolean shouldRemoveSubBagsWhenEmpty) {
		this(shouldRemoveSubBagsWhenEmpty, null);
	}

	public DuplicatedKeysTreeMap(boolean shouldRemoveSubBagsWhenEmpty,
			Comparator<? super V> comparator) {
		this.comparator = comparator;
		this.shouldRemoveSubBagsWhenEmpty = shouldRemoveSubBagsWhenEmpty;
	}

	/**
	 * Retrieves and removes the first element for the given key or returns
	 * <code>null<code> if the key is unknown.
	 * 
	 * @param key
	 * @return
	 */
	public V poll(K key) {
		Queue<V> bag = map.get(key);
		V retValue = null;

		if (bag != null) {
			retValue = bag.poll();

			if (shouldRemoveSubBagsWhenEmpty && bag.isEmpty()) {
				map.remove(key);
			}
		}

		return retValue;
	}

	/**
	 * Retrieves and removes the first element for the least key greater than or
	 * equal to the given key or returns <code>null<code> if there is no such
	 * key.
	 * 
	 * @param minimumKey
	 * @return
	 */
	public V pollCeiling(K minimumKey) {
		Entry<K, Queue<V>> entry = map.ceilingEntry(minimumKey);
		V retValue = null;

		if (entry != null) {
			Queue<V> bag = entry.getValue();
			retValue = bag.poll();

			if (shouldRemoveSubBagsWhenEmpty && bag.isEmpty()) {
				map.remove(entry.getKey());
			}
		}

		return retValue;
	}

	/**
	 * Adds the given value to the bag of values associated with the given key.
	 * 
	 * @param key
	 * @param value
	 */
	public void put(K key, V value) {
		Queue<V> bag = map.get(key);

		if (bag == null) {
			if (comparator == null)
				bag = new PriorityQueue<V>();
			else
				bag = new PriorityQueue<V>(comparator);
			map.put(key, bag);
		}

		bag.add(value);
	}

}