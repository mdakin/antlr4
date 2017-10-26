package org.antlr.v4.runtime.dfa.edgemap;

import java.util.Arrays;

/**
 * A simple hashmap with integer keys and T values.
 * implements open address linear probing algorithm.
 * <p>
 * Constraints:
 * - Supports int key values in range (Integer.MIN_VALUE..Integer.MAX_VALUE];
 * - Does not implement Map interface
 * - Size can be max 1 << 29
 * - Does not support remove.
 * - Does not implement Iterable.
 * - Class is not thread safe.
 */
public final class SymbolMap<T> extends BaseEdgeMap<T> {

	/**
	 * Capacity of the map is expanded when size reaches to
	 * capacity * LOAD_FACTOR.
	 */
	private static final float LOAD_FACTOR = 0.65f;

	// When size reaches a threshold, backing arrays are expanded.
	private int threshold;

	/**
	 * @param capacity initial internal array size. It must be a positive
	 * number. If value is not a power of two, size will be the nearest
	 * larger power of two.
	 */
	@SuppressWarnings("unchecked")
	public SymbolMap(int capacity) {
		capacity = adjustCapacity(capacity) ;
		keys = new int[capacity];
		values = (T[]) new Object[keys.length];
		Arrays.fill(keys, EMPTY);
		modulo = keys.length - 1;
		threshold = (int) (capacity * LOAD_FACTOR);
	}

	public int capacity() {
		return keys.length;
	}

	public int size() {
		return keyCount;
	}

	private void checkKey(int key) {
		if (key == EMPTY) {
			throw new IllegalArgumentException("Illegal key: " + key);
		}
	}

	public boolean put(int key, T value) {
		checkKey(key);
		if (keyCount == threshold) {
			// Caller should create a new version with expanded capacity.
			return false;
		}
		int loc = locate(key);
		if (loc >= 0) {
			values[loc] = value;
		} else {
			loc = -loc - 1;
			keys[loc] = key;
			values[loc] = value;
			keyCount++;
		}
		return true;
	}

	/**
	 * @return The value {@code T} taht is mapped to given {@code key}.
	 * or  {@code null} If key does not exist.
	 *
	 * @throws IllegalArgumentException if key is {@code Integer.MIN_INT}
	 */
	public T get(int key) {
		checkKey(key);
		int slot = key & modulo;
		if (key == keys[slot]) return values[slot];
		while (true) {
			slot = (slot + 1) & modulo;
			final int t = keys[slot];
			if (t == key) {
				return values[slot];
			}
			if (t == EMPTY) {
				return null;
			}
		}
	}

	public boolean containsKey(int key) {
		return locate(key) >= 0;
	}

	private int locate(int key) {
		int slot = key & modulo;
		while (true) {
			int k = keys[slot];
			// If slot is empty, return its location
			if (k == EMPTY) {
				return -slot - 1;
			}
			if (k == key) {
				return slot;
			}
			slot = (slot + 1) & modulo;
		}
	}

	private int newCapacity() {
		long size = (long) (keys.length * 2);
		if (keys.length > CAPACITY_LIMIT) {
			throw new RuntimeException("Map size is too large.");
		}
		return (int) size;
	}

	/**
	 * Expands backing arrays by doubling their capacity.
	 */
	public EdgeMap<T> expand() {
		int capacity = newCapacity();
		SymbolMap<T> newMap = new SymbolMap<>(capacity);
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != EMPTY) {
				newMap.put(keys[i], values[i]);
			}
		}
		return newMap;
	}
}
