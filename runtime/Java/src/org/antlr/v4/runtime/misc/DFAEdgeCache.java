package org.antlr.v4.runtime.misc;

import java.util.Arrays;
import org.antlr.v4.runtime.dfa.DFAState;

public class DFAEdgeCache {

	private static final int DEFAULT_INITIAL_CAPACITY = 2;

	private static final int DEFAULT_MAX_CAPACITY = 1 << 7;

	private static final int CAPACITY_LIMIT = 1 << 29;

	// Special value to mark empty cells.
	private static final int EMPTY = Integer.MIN_VALUE;

	// volatile guarantees atomic reference copy.
	// Initial map is the perfect map that expands in case of a collision.
  // If it reaches to a certain size, it is replaced with normal int map that only grows
	// when its size reaches to a certain threshold. This map uses linear probing and
	// expands only size reaches to a certain threshold, until capacity reaches to CAPACITY_LIMIT
	// after that it throws.
	private volatile EdgeCache<DFAState> edgeMap;

	public DFAEdgeCache() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_MAX_CAPACITY);
	}

	public DFAEdgeCache(int initialCapacity) {
		this(initialCapacity, DEFAULT_MAX_CAPACITY);
	}

	public DFAEdgeCache(int initialCapacity, int maxCapacity) {
		edgeMap = new TinyPerfectMap<>(initialCapacity, maxCapacity);
	}

	public synchronized void addEdge(int symbol, DFAState state) {
		while(!edgeMap.put(symbol, state)) {
  		EdgeCache<DFAState> newMap = edgeMap.expand();
			// If we fail to insert even if we expand, we switch to use a (non perfect) hashmap.
			if (edgeMap == newMap) {
				newMap = new SymbolMap<>(edgeMap.size());
				int[] keys = edgeMap.getKeys();
				Object[] values = edgeMap.getValues();
				for (int i=0; i<keys.length; i++) {
					newMap.put(keys[i], (DFAState)values[i]);
				}
			}
			// Replace the map with new version.
			edgeMap = newMap;
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

	public int[] getKeys() {return edgeMap.getKeys();}

	/**
	 * A small map with int keys. Properties:
	 * - get is always a single lookup.
	 * - put may fail.
	 * - Is not thread safe
	 */
	final static private class TinyPerfectMap<T> implements EdgeCache<T>{
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

		private int hash(int x) {
			final int h = x * 0x9E3779B9; // int phi
			return (h ^ (h >> 16)) & modulo;
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

		/**
		 * @return The value {@code T} taht is mapped to given {@code key}.
		 * or  {@code null} If key does not exist.
		 *
		 * @throws IllegalArgumentException if key is {@code Integer.MIN_INT}
		 */
		public T get(int key) {
			checkKey(key);
			final int loc = key & modulo;
			return keys[loc] == key ?  values[loc] : null;
		}

		public boolean containsKey(int key) {
			return get(key) != null;
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
		 * @return The array of keys in the map.
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

		/**
		 * Try to create a new map with double capacity.
		 */
		public EdgeCache<T> expand() {
			TinyPerfectMap<T> newMap = this;
			// Expand the map until there are no collisions. Or capacity reaches to maxCapacity.
			int capacity = keys.length;
			boolean allFits = false;
			expandAgain:
			while (!allFits && capacity <= maxCapacity) {
				capacity = capacity * 2;
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
				allFits = true;
			}
			return allFits ? newMap : this;
		}

	}

}
