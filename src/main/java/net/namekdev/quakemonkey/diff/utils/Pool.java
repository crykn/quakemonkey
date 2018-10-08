package net.namekdev.quakemonkey.diff.utils;

import java.util.PriorityQueue;
import java.util.Queue;

import com.google.common.base.Preconditions;

public class Pool<T> {
	private final Queue<T> _bag;
	private ObjectSupplier<T> _objServicer;

	public Pool(ObjectSupplier<T> objServicer, int size) {
		_bag = new PriorityQueue<T>(size);
		_objServicer = objServicer;
	}

	public Pool(ObjectSupplier<T> objServicer) {
		this(objServicer, 127);
	}

	/**
	 * 
	 * @return Obtains an object reference saved in the pool or a newly
	 *         instantiated object if the pool is empty.
	 */
	public T obtain() {
		synchronized (_bag) {
			T item = _bag.poll();

			if (item == null) {
				item = _objServicer.onCreate();
			}
			// _objServicer.onGet(item);

			return item;
		}
	}

	/**
	 * Frees the given object to be used by {@link #obtain()} again.
	 * 
	 * @param obj
	 */
	public void free(T obj) {
		Preconditions.checkNotNull(obj);

		synchronized (_bag) {
			_objServicer.onFree(obj);
			_bag.add(obj);
		}
	}

	public static interface ObjectSupplier<T> {
		public T onCreate();

		public void onFree(T obj);
	}
}
