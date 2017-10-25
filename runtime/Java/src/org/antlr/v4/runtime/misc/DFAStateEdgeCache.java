package org.antlr.v4.runtime.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.antlr.v4.runtime.dfa.DFAState;

public class DFAStateEdgeCache {

	private static final int DEFAULT_INITIAL_CAPACITY = 4;

	private static final int DEFAULT_MAX_CAPACITY = 1 << 9;

	private static final int CAPACITY_LIMIT = 1 << 29;

	// Special value to mark empty cells.
	private static final int EMPTY = Integer.MIN_VALUE;

	// volatile guarantees atomic reference copy.
	private volatile TinyPerfectMap<DFAState> edgeMap;

	public DFAStateEdgeCache() {
		this(DEFAULT_INITIAL_CAPACITY);
	}

	public DFAStateEdgeCache(int initialCapacity) {
		edgeMap = new TinyPerfectMap<DFAState>(initialCapacity, DEFAULT_MAX_CAPACITY);
	}

	public DFAStateEdgeCache(int initialCapacity, int maxCapacity) {
		edgeMap = new TinyPerfectMap<DFAState>(initialCapacity, maxCapacity);
	}

	public synchronized boolean addEdge(int symbol, DFAState state) {
		while(true) {
			if (!edgeMap.put(symbol, state)) {
				TinyPerfectMap<DFAState> newMap = edgeMap.expand();
				// Fail if can not expand anymore.
				if (edgeMap == newMap) {
					return false;
				}
				// Replace the map with new version.
				edgeMap = newMap;
			}
		}
	}

	public DFAState getState(int symbol) {
		return edgeMap.get(symbol);
	}

	public int size() {
		return edgeMap.size();
	}

	public int capacity() {
		return edgeMap.capacity();
	}

	/**
	 * A small map with int keys. Properties:
	 * - get does at most 2 lookups. (Allows a single collision)
	 * - Does not guarantee adding a key-value, if key > MAX_CAPACITY.
	 * - Is not thread safe
	 * - Does not support deletion
	 *
	 */
	final class TinyPerfectMap<T> {
		// Backing arrays for keys and value references.
		private int[] keys;
		private T[] values;

		// Number of keys in the map = size of the map.
		private int keyCount;

		/**
		 * Map capacity is always a power of 2. With this property,
		 * integer modulo operation (key % capacity) can be replaced with
		 * (key & (capacity - 1)). We keep (capacity - 1) value in this variable.
		 */
		private int modulo;

		private int maxCapacity;

		public TinyPerfectMap() {
			this(DEFAULT_INITIAL_CAPACITY, DEFAULT_MAX_CAPACITY);
		}

		/**
		 * @param capacity initial internal array size. It must be a positive
		 * number. If value is not a power of two, size will be the nearest
		 * larger power of two.
		 */
		@SuppressWarnings("unchecked")
		public TinyPerfectMap(int capacity, int maxCapacity) {
			capacity = adjustCapacity(capacity) ;
			this.maxCapacity = adjustCapacity(maxCapacity);
			keys = new int[capacity];
			values = (T[]) new Object[keys.length];
			Arrays.fill(keys, EMPTY);
			modulo = keys.length - 1;
		}

		private int adjustCapacity(int capacity) {
			if (capacity < 1) {
				throw new IllegalArgumentException("Capacity must be > 0: " + capacity);
			}
			long k = 1;
			while (k < capacity) {
				k <<= 1;
			}
			if (k > CAPACITY_LIMIT) {
				throw new IllegalArgumentException("Size too large: " + capacity);
			}
			return (int) k;
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

		private int hash1(int key) {
			return key & modulo;
		}

		private int hash2(int x) {
			final int h = x * 0x9E3779B9; // int phi
			return (h ^ (h >> 16)) & modulo;
		}

		public boolean put(int key, T value) {
			checkKey(key);
			// First hash is the directly key % capacity, for key space with a bounded upper limit
			// This guarantees capacity will be at most the first 2^n bigger than the limit. e.g.
			// For ascii input (0..127), this guarantees capacity will be maximum 256 and all keys
			// will fit and can be reached with a single lookup.
			int loc = hash1(key);
			if (putOrUpdate(loc, key, value)) {
				return true;
			}
			// If we fail to put the element with first hash, we try a different hash. This
			// provides a second chance to put a key if there is a collision, especially
			// if key space upper limit is a large value (e.g. non ascii input)
			int loc2 = hash2(key);
			if (putOrUpdate(loc2, key, value)) {
				return true;
			}
			// Could not insert key, value.
			return false;
		}

		private boolean putOrUpdate(int loc, int key, T value) {
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
			// Could not put ot update.
			return false;
		}

		/**
		 * @return The value {@code T} taht is mapped to given {@code key}.
		 * or  {@code null} If key does not exist.
		 *
		 * @throws IllegalArgumentException if key is {@code Integer.MIN_INT}
		 */
		public T get(int key) {
			checkKey(key);
			int loc = hash1(key);
			if (keys[loc] == key) {
				return values[loc];
			}
			if (keys[loc] == EMPTY) {
				return  null;
			}
			// first lookup failed with collision, try again with a different hash.
			loc = hash2(key);
			if (keys[loc] == key) {
  			return values[loc];
			}
			return null;
		}

		public boolean containsKey(int key) {
			return get(key) != null;
		}

		/**
		 * @return The array of keys in the map. Sorted ascending.
		 */
		public int[] getKeys() {
			int[] keyArray = new int[keyCount];
			int c = 0;
			for (int key : keys) {
				if (key != EMPTY) {
					keyArray[c++] = key;
				}
			}
			Arrays.sort(keyArray);
			return keyArray;
		}

		/**
		 * @return The array of keys in the map. Sorted ascending.
		 */
		public List<T> getValues() {
			List<T> result = new ArrayList<>();
			for (int i = 0; i < keys.length; i++) {
				if (keys[i] >= 0) {
					result.add(values[i]);
				}
			}
			return result;
		}

		/**
		 * Try to create a new map with double capacity.
		 */
		private TinyPerfectMap<T> expand() {
			TinyPerfectMap<T> newMap = this;
			int capacity = keys.length * 2;
			if (capacity > maxCapacity) {
				return this;
			}
			boolean allFit = false;
			// Expand the map until there are no collisions. Or capacity reaches to MAX_CAPACITY
			expandAgain:
			while(capacity <= maxCapacity) {
				newMap = new TinyPerfectMap<>(capacity, maxCapacity);
				// Try to insert all key-values into new array
				for (int i = 0; i < keys.length; i++) {
					if (keys[i] != EMPTY) {
						// If we can not insert, expand again.
						if (!newMap.put(keys[i], values[i])) {
							continue expandAgain;
						}
					}
				}
				allFit = true;
			}
			// We can not fit all elements into new map, try to put all existing values, starting with
			// elements with small key values
			if (!allFit) {
				// First fill the map with keys < capacity, giving precedence to smaller keys.
				for (int i = 0; i < keys.length; i++) {
					if (keys[i] != EMPTY && keys[i] < keys.length) {
						 newMap.put(keys[i], values[i]);
					}
				}
				// Then try to put remaining keys, ignore failures.
				for (int i = 0; i < keys.length; i++) {
  				newMap.put(keys[i], values[i]);
				}
			}
			return newMap;
		}

	}
}
