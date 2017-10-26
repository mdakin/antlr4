package org.antlr.v4.runtime.dfa.edgemap;

import java.util.Arrays;

/**
 * A small map that guarantees single lookups for get operations (no collisions). put operation
 * fails if it encounters a collision, the caller side is then may call expand method to create
 * a new map with no collisions, expand may also fail to create a collision free map. It is
 * callers responsibility to switch to a new map type.
 *
 * For an input key space [0..n] This map only grows up to 2^x capacity that is bigger than n, which
 * makes it basically a lookup table if keys are always small (e.g. all are ascii characters or
 * all are token ids). It generally uses less memory than a sparse lookup table if size of map is
 * small.
 *
 * This map is not thread safe and designed to be managed by a thread safe caller class.
 */
final class PerfectEdgeMap<T> extends BaseEdgeMap<T> {

	private int maxCapacity;
	/**
	 * @param capacity initial internal array capacity. It must be a positive number. If value is not a
	 * power of two, size will be the nearest larger power of two.
	 */
	@SuppressWarnings("unchecked")
	PerfectEdgeMap(int capacity, int maxCapacity) {
		capacity = adjustInitialCapacity(capacity);
		this.maxCapacity = adjustInitialCapacity(maxCapacity);
		keys = new int[capacity];
		values = (T[]) new Object[keys.length];
		Arrays.fill(keys, EMPTY);
		modulo = keys.length - 1;
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
		int loc = key & modulo;
		// If slot is empty, just put the value
		if (keys[loc] == EMPTY) {
			keys[loc] = key;
			values[loc] = value;
			keyCount++;
			return true;
		}
		// If slot has same key, update it with new value
		if (keys[loc] == key) {
			values[loc] = value;
			return true;
		}
		// Put failed.
		return false;
	}

	public T get(int key) {
		checkKey(key);
		final int loc = key & modulo;
		return keys[loc] == key ? values[loc] : null;
	}

	public boolean containsKey(int key) {
		return get(key) != null;
	}

	/**
	 * Tries to create a new map with double capacity. If it fails to create a new expanded map
	 * (in case it can not create a map with no collisions) it returns this.
	 */
	public EdgeMap<T> expand() {
		PerfectEdgeMap<T> newMap = this;
		// Expand the map until there are no collisions. Or capacity reaches to maxCapacity.
		int capacity = keys.length;
		boolean allFits = false;
		expandAgain:
		while (!allFits && capacity <= maxCapacity) {
			capacity = capacity * 2;
			newMap = new PerfectEdgeMap<>(capacity, maxCapacity);
			// Try to insert all key-values into new array
			for (int i = 0; i < keys.length; i++) {
				if (keys[i] != EMPTY) {
					// If we can not insert, expand again.
					if (!newMap.put(keys[i], values[i])) {
						continue expandAgain;
					}
				}
			}
			allFits = true;
		}
		return allFits ? newMap : this;
	}

}
