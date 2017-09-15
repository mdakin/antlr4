package org.antlr.v4.runtime.dfa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple hashmap with integer keys and T values.
 * Implementation is open address linear probing.
 * <p>
 * Constraints:
 * - Only support key values in range (Integer.MIN_VALUE..Integer.MAX_VALUE];
 * - Size can be max 1 << 30
 * - Does not support remove.
 * - Does not implement Iterable.
 * - Class is not thread safe.
 */
public final class SimpleIntMap<T> {
	private static final int DEFAULT_INITIAL_SIZE = 8;
	// specifically selected to fit max 5,10 elements to 8,16 sized maps.
	private static final double LOAD_FACTOR = 0.65;
	private static final int MAX_SIZE = 1 << 30;
	// Special value to mark empty cells.
	private static final int EMPTY = Integer.MIN_VALUE;
	private static final int MIN_KEY_VALUE = Integer.MIN_VALUE;

	// Backing arrays for keys and value references.
	private int[] keys;
	private T[] values;

	// Number of keys in the map = size of the map.
	private int keyCount;
	// When size reaches a threshold, backing arrays are expanded.
	private int threshold;
	// Map size is always a power of 2. With this property,
	// integer modulo operation (x % size) can be replaced with
	// (x & (size - 1)) and we keep (size - 1) value in this variable.
	private int modulo;

	public SimpleIntMap() {
		this(DEFAULT_INITIAL_SIZE);
	}

	/**
	 * @param size initial internal array size. It must be a positive number.
	 *     If value is not a power of two, size will be the nearest larger power of two.
	 */
	public SimpleIntMap(int size) {
		size = adjustInitialSize(size) ;
		keys = new int[size];
		values = (T[]) new Object[keys.length];
		Arrays.fill(keys, EMPTY);
		modulo = keys.length - 1;
		threshold = (int) (size * LOAD_FACTOR);
	}

	private int adjustInitialSize(int size) {
		if (size < 1) {
			throw new IllegalArgumentException("Size must > 0: " + size);
		}
		long k = 1;
		while (k < size) {
			k <<= 1;
		}
		return (int) k;
	}

	public int size() {
		return keyCount;
	}

	private int initialProbe(final int hashCode) {
 	  return hashCode >= 0 ? hashCode & modulo : -hashCode & modulo;
	}

	private int probeNext(int index) {
		return index & modulo;
	}

	private void checkKey(int key) {
		if (key <= MIN_KEY_VALUE) {
			throw new IllegalArgumentException("Illegal key: " + key);
		}
	}

	public void put(int key, T value) {
		checkKey(key);
		if (keyCount == threshold) {
			expand();
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
	}

	/**
	 * Returns the value associated with given key.
	 * If key does not exist, returns null.
	 * <p>
	 * For key = Integer.MIN_INT behavior is undefined.
	 */
	public T get(int key) {
		int slot = initialProbe(key);
		// Test the lucky first shot. (>99% of cases in case of antlr4)
		if (key == keys[slot]) {
			return values[slot];
		}
		// Continue linear probing otherwise
		while (true) {
			slot = probeNext(slot + 1);
			final int t = keys[slot];
			if (t == key) {
				return values[slot];
			}
			if (t == EMPTY) {
				return null;
			}
		}
	}

	public int[] getKeys() {
		int[] keyArray = new int[keyCount];
		int c = 0;
		for (int key : keys) {
			if (key > MIN_KEY_VALUE) {
				keyArray[c++] = key;
			}
		}
		return keyArray;
	}

	public List<T> getValues() {
		List<T> result = new ArrayList<>();
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] >= 0) {
				result.add(values[i]);
			}
		}
		return result;
	}

	private int locate(int key) {
		int slot = initialProbe(key);
		while (true) {
			final int k = keys[slot];
			// If slot is empty, return its location
			if (k == EMPTY) {
				return -slot - 1;
			}
			if (k == key) {
				return slot;
			}
			slot = probeNext(slot + 1);
		}
	}

	public boolean containsKey(int key) {
		return locate(key) >= 0;
	}

	private int newSize() {
		long size = (long) (keys.length * 2);
		if (keys.length > MAX_SIZE) {
			throw new RuntimeException("Map size is too large.");
		}
		return (int) size;
	}

	private void expand() {
		int size = newSize();
		SimpleIntMap<T> h = new SimpleIntMap<>(size);
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] > MIN_KEY_VALUE) {
				h.put(keys[i], values[i]);
			}
		}
		this.keys = h.keys;
		this.values = h.values;
		this.threshold = h.threshold;
		this.modulo = h.modulo;
	}
}
