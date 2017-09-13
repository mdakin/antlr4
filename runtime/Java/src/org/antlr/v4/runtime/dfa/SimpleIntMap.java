package org.antlr.v4.runtime.dfa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple hashmap with integer keys and T values.
 * Implementation is open address linear probing with some heuristics on
 * expansion limits. Some parts of the map is optimized for tiny maps.
 *
 * Constraints:
 * - Only support key values in range (Integer.MIN_VALUE..Integer.MAX_VALUE];
 * - Size can be max 1 << 30
 * - Does not support remove.
 * - Does not implement Iterable.
 * - Class is not thread safe.
 *
 * Note: If contains mostly positive or mostly negative numbers it is very fast,
 * but if there are lots of abs(key) collisions, performance may degenerate
 * considerably. (e.g. all keys in [-range..range])
 */
public class SimpleIntMap<T> {
    private static final int DEFAULT_INITIAL_SIZE = 2;
    private static final double LOAD_FACTOR = 0.8;
    // Very small maps are traversed linearly and doubles size on expand.
    private static final int TINY_SIZE = 8;
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
    private int modulo;

    public SimpleIntMap() {
        this(DEFAULT_INITIAL_SIZE);
    }

    public SimpleIntMap(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Size must > 0: " + size);
        }
        // TODO: make size power of 2
        keys = new int[size];
        values = (T[]) new Object[keys.length];
        Arrays.fill(keys, EMPTY);
        modulo = keys.length - 1;
        threshold = size <= TINY_SIZE ? size : (int) (size * LOAD_FACTOR);
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

    private T getTiny(int key) {
        for (int i=0; i < keys.length; i++) {
            if (keys[i] == key) return values[i];
        }
        return null;
    }

    /**
	 * Returns the value associated with given key.
	 * If key does not exist, returns null.
	 *
	 * For key = Integer.MIN_INT behavior is undefined.
	 */
    public T get(int key) {
        // For tiny maps, just go through all keys.
        if (keys.length <= TINY_SIZE) {
            return getTiny(key);
        }
        // Else apply linear probing.
        int slot = initialProbe(key);
        while (true) {
            final int t = keys[slot];
            if (t == EMPTY) {
                return null;
            }
            if (t == key) {
                return values[slot];
            }
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
            slot = probeNext(slot+1);
        }
    }

    public boolean containsKey(int key) {
        return locate(key) >= 0;
    }

    private int newSize() {
        long size = (long)(keys.length * 2);
		if (keys.length > MAX_SIZE) {
			throw new RuntimeException("Map size is too large.");
		}
        return (int) size;
    }

    private void expand() {
        SimpleIntMap<T> h = new SimpleIntMap<>(newSize());
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

    public String rawKeysString() {
    	StringBuilder sb = new StringBuilder();
    	for (int i=0; i<keys.length; i++) {
    		if (keys[i] == EMPTY) {
    			sb.append("_, ");
			} else {
    			sb.append(keys[i]).append(", ");
			}
		}
		sb.append(" ").append(keys.length).append(":").append(keyCount);
		return sb.toString();
	}
}
