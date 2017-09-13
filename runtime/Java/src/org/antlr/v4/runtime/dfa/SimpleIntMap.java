package org.antlr.v4.runtime.dfa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple hashmap with integer keys and T values.
 * Implementation is open address linear probing.
 * For tiny maps uses linear search.
 * <p>
 * Constraints:
 * - Only support key values in range (Integer.MIN_VALUE..Integer.MAX_VALUE];
 * - Size can be max 1 << 30
 * - Does not support remove.
 * - Does not implement Iterable.
 * - Class is not thread safe.
 */
public class SimpleIntMap<T> {
	private static final int DEFAULT_INITIAL_SIZE = 8;
	private static final double LOAD_FACTOR = 0.7;
	// Very small maps are traversed linearly and expand() does not double its size.
	private static final int TINY_SIZE = 16;
	// This value is VM dependent. Slightly smaller than the one in ArrayList.
	private static final int MAX_SIZE = 1 << 30;
	// Special value to mark empty cells.
	private static final int EMPTY = Integer.MIN_VALUE;
	private static final int MIN_KEY_VALUE = Integer.MIN_VALUE;

	// Backing arrays for keys and value references.
	private int[] keys;
	private T[] values;

	// Number of keys in the map. Size of the map.
	private int keyCount;
	// When size reaches a threshold, backing arrays are expanded.
	private int threshold;
	// Except for the tiny maps, map size is always a power of 2,
	// integer modulo operation x % size can be replaced with
	// x & (size - 1) and we keep size - 1 value in this variable.
	private int modulo;
	// If the map is too small, we use different algorithms:
	// - get is a linear search
	// - put only inserts keys at the beginning of array
	// - map is expanded slowly
	private boolean isTiny;

	public class Instrumentation {
		public long collisions;
		public long skips;
		public long totalTinyGet;
		public long totalProbeGet;
		public long[] tinyHits = new long[10];
		public long[] probeHits = new long[10];

		public void updateTinyHits(int i) {
			tinyHits[Math.min(i, tinyHits.length - 1)]++;
		}

		public void updateProbeHits(int i) {
			probeHits[Math.min(i, probeHits.length - 1)]++;
		}
	}

	public Instrumentation ins = new Instrumentation();

	public SimpleIntMap() {
		this(DEFAULT_INITIAL_SIZE);
	}

	/**
	 * @param size initial internal array size. It must be a positive number. If value is not a power of two, size will
	 *             ne the nearest larger power of two.
	 */
	public SimpleIntMap(int size) {
		size = adjustInitialSize(size) ;
		keys = new int[size];
		values = (T[]) new Object[keys.length];
		Arrays.fill(keys, EMPTY);
		modulo = keys.length - 1;
		isTiny = size <= TINY_SIZE;
		// For tiny maps, threshold is equal to size.
		threshold = isTiny ?  size : (int) (size * LOAD_FACTOR);
	}

	private int adjustInitialSize(int size) {
		if (size < 1) {
			throw new IllegalArgumentException("Size must > 0: " + size);
		}
		// If map size is tiny, use the size as is.
		if (size <= TINY_SIZE) {
			return size;
		}
		// For bigger maps, adjust to nearest 2^n size.
		long k = 1;
		while (k < size) {
			k <<= 1;
		}
		return (int) k;
	}

	public int size() {
		return keyCount;
	}

	private int initialProbe(int hashCode) {
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
		if (isTiny) {
			putTiny(key, value);
			return;
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

	private void putTiny(int key, T value) {
		for (int i = 0; i < keyCount; i++) {
			if (keys[i] == key) {
				values[i] = value;
				return;
			}
		}
		keys[keyCount] = key;
		values[keyCount] = value;
		keyCount++;
	}

	private T getTiny(int key) {
		for (int i = 0; i < keyCount; i++) {

			if (keys[i] == key) {
				ins.updateTinyHits(i);
				return values[i];
			}
			else ins.skips++;
		}
		return null;
	}

	/**
	 * Returns the value associated with given key.
	 * If key does not exist, returns null.
	 * <p>
	 * For key = Integer.MIN_INT behavior is undefined.
	 */
	public T get(int key) {
		// For tiny maps, look keys from [0..keyCount-1].
		if (isTiny) {
			ins.totalTinyGet++;
			return getTiny(key);
		}
		ins.totalProbeGet++;
		// Else apply linear probing.
		int slot = initialProbe(key);
		int steps = 0;
		while (true) {
			final int t = keys[slot];
			if (t == EMPTY) {
				return null;
			}
			if (t == key) {
				ins.updateProbeHits(steps);
				return values[slot];
			}
			steps++;
			ins.collisions++;
			slot = probeNext(slot + 1);
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
		// If map is tiny, expand slowly.
		if (isTiny) {
			return keys.length + 4;
		}
		// Otherwise double the size.
		long size = (long) (keys.length * 2);
		if (keys.length > MAX_SIZE) {
			throw new RuntimeException("Map size is too large.");
		}
		return (int) size;
	}

	private void expand() {
		int size = newSize();

		// For smaller size just expand and copy arrays.
		if (size <= TINY_SIZE) {
			this.keys = Arrays.copyOf(keys, size);
			this.values = Arrays.copyOf(values, size);
			this.threshold = size;
			return;
		}

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
		this.isTiny = h.isTiny;
	}

	public String toDebugString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Size: ").append(keys.length).append(":").append(keyCount);
		if (isTiny) {
			sb.append(" *Tiny*");
		}
		sb.append('\n');
		sb.append("Collisions: ").append(ins.collisions).append('\n');
		sb.append("Tiny skips: ").append(ins.skips).append('\n');
		sb.append("Total tiny gets : ").append(ins.totalTinyGet).append('\n');
		sb.append("Total probe gets: ").append(ins.totalProbeGet).append('\n');
		sb.append("Tiny hit histogram:  ");
		for (int i = 0; i < ins.tinyHits.length; i++) {
			sb.append(i + ": " + ins.tinyHits[i]).append(", ");
		}
		sb.append('\n');
		sb.append("Probe hit histogram: ");
		for (int i = 0; i < ins.probeHits.length; i++) {
			sb.append(i + ": " + ins.probeHits[i]).append(", ");
		}
//		sb.append('\n');
//		for (int key : keys) {
//			if (key == EMPTY) {
//				sb.append("_, ");
//			} else {
//				sb.append(key).append(", ");
//			}
//		}
		return sb.toString();
	}

}
