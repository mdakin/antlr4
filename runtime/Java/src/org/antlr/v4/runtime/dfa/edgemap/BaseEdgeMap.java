package org.antlr.v4.runtime.dfa.edgemap;

public abstract class BaseEdgeMap<T> implements EdgeMap<T> {

	// Special value to mark empty cells.
	static final int EMPTY = Integer.MIN_VALUE;

	static final int CAPACITY_LIMIT = 1 << 29;

	/**
	 * Map capacity is always a power of 2. With this property,
	 * integer modulo operation (key % capacity) can be replaced with
	 * (key & (capacity - 1)). We keep (capacity - 1) value in this variable.
	 */
	int modulo;

	// Backing arrays for keys and value references.
	protected int[] keys;
	protected T[] values;

	// Number of keys in the map = size of the map.
	int keyCount;

	protected int adjustInitialCapacity(int initialCapacity) {
		if (initialCapacity < 1) {
			throw new IllegalArgumentException("Capacity must be > 0: " + initialCapacity);
		}
		long k = 2;
		while (k < initialCapacity) {
			k <<= 1;
		}
		if (k > CAPACITY_LIMIT) {
			throw new IllegalArgumentException("Size too large: " + initialCapacity);
		}
		return (int) k;
	}

	/**
	 * @return The array of keys in the map.
	 */
	public int[] getKeys() {
		int[] keyArray = new int[keyCount];
		int c = 0;
		for (int key : keys) {
			if (key != EMPTY) {
				keyArray[c++] = key;
			}
		}
		return keyArray;
	}

	/**
	 * @return The array of values in the map (shares same order with getKeys).
	 */
	@SuppressWarnings("unchecked")
	public T[] getValues() {
		T[] valueArray = (T[]) new Object[keyCount];
		for (int i = 0, j = 0; i < keys.length; i++) {
			if (keys[i] != EMPTY) {
				valueArray[j++] = values[i];
			}
		}
		return valueArray;
	}


}
