package net.namekdev.quakemonkey.utils.pool;

import java.util.PriorityQueue;
import java.util.Queue;

import com.google.common.base.Preconditions;

public class Pool<T> {
	private final Queue<T> objPool;
	private final ObjectSupplier<T> objSupplier;

	public Pool(ObjectSupplier<T> objSupplier, int size) {
		objPool = new PriorityQueue<T>(size);
		this.objSupplier = objSupplier;
	}

	public Pool(ObjectSupplier<T> objSupplier) {
		this(objSupplier, 127);
	}

	/**
	 * 
	 * @return Obtains an object reference saved in the pool or a newly
	 *         instantiated object if the pool is empty.
	 */
	public T obtain() {
		synchronized (objPool) {
			T item = objPool.poll();

			if (item == null) {
				item = objSupplier.get();
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

		synchronized (objPool) {
			objSupplier.onFree(obj);
			objPool.add(obj);
		}
	}

	public static interface ObjectSupplier<T> {
		public T get();

		public void onFree(T obj);
	}
}