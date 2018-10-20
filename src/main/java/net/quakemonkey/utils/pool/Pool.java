package net.quakemonkey.utils.pool;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.base.Preconditions;

/**
 * A simple pool implementation for objects.
 *
 * @param <T>
 * @see ObjectSupplier
 */
public class Pool<T> {
	private final Queue<T> objPool;
	private final ObjectSupplier<T> objSupplier;

	public Pool(ObjectSupplier<T> objSupplier, int size) {
		objPool = new LinkedBlockingQueue<T>(size);
		this.objSupplier = objSupplier;
	}

	public Pool(ObjectSupplier<T> objSupplier) {
		this(objSupplier, 127);
	}

	/**
	 * @return Obtains an object reference saved in the pool or a newly
	 *         instantiated object if the pool is empty.
	 */
	public T obtain() {
		T item = objPool.poll();

		if (item == null) {
			item = objSupplier.newInstance();
		}
		// _objServicer.onGet(item);

		return item;
	}

	/**
	 * Frees the given object to be used by {@link #obtain()} again.
	 * 
	 * @param obj
	 */
	public void free(T obj) {
		Preconditions.checkNotNull(obj);

		objSupplier.onFree(obj);
		objPool.add(obj);
	}

	/**
	 * This object supplier takes care of creating new and freeing obtained
	 * objects in a pool.
	 * 
	 * @param <T>
	 */
	public static interface ObjectSupplier<T> {
		public T newInstance();

		public void onFree(T obj);
	}
}
