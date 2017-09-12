package org.antlr.v4.runtime.dfa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple hashmap with integer keys and T values.
 * Implementation is open address linear probing with some heuristics on
 * expansion limits. For tiny maps uses linear
 *
 * Constraints:
 * - Only support key values in range (Integer.MIN_VALUE..Integer.MAX_VALUE];
 * - Size can be max ~Integer.MAX_VALUE * LOAD_FACTOR
 * - Does not support remove.
 * - Does not implement Iterable.
 * - This class is not thread safe.
 *
 * Note: If contains mostly positive or mostly negative numbers it is very fast,
 * but if there are lots of abs(key) collisions, performance may degenerate
 * considerably. (e.g. all keys in [-range..range])
 */
public class SimpleIntMap<T> {
    private static final int DEFAULT_INITIAL_SIZE = 4;
    private static final double LOAD_FACTOR = 0.8;
    private static final double GROWTH_FACTOR = 1.8;
    // Very small maps are traversed linearly and doubles size on expand.
    private static final int TINY_SIZE_TRIGGER = 12;
    // This value is VM dependent. Slightly smaller than the one in ArrayList.
    private static final int MAX_SIZE = Integer.MAX_VALUE - (1 << 10);
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

    public SimpleIntMap() {
        this(DEFAULT_INITIAL_SIZE);
    }

    public SimpleIntMap(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Size must > 0: " + size);
        }
        keys = new int[size];
        values = (T[]) new Object[keys.length];
        Arrays.fill(keys, EMPTY);
        threshold = (int) (size * LOAD_FACTOR);
    }

    public int size() {
        return keyCount;
    }

    private int initialProbe(int hashCode) {
        return hashCode >= 0 ? hashCode % keys.length : -hashCode % keys.length;
    }

    private int probeNext(int index) {
        return index  % keys.length;
    }

    private void checkKey(int key) {
        if (key <= MIN_KEY_VALUE) {
            throw new IllegalArgumentException("Illegal key: " + key);
        }
    }

    private T getTiny(int key) {
        for (int i=0; i < keys.length; i++) {
            if (keys[i] == key) return values[i];
        }
        return null;
    }

    /** Returns the value associated with given key. If key does not exist, returns null. */
    public T get(int key) {
        checkKey(key);
        // For tiny maps, just go through all keys.
        if (keys.length < TINY_SIZE_TRIGGER) {
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
        // If size is tiny, double it
        if (keys.length < TINY_SIZE_TRIGGER) {
            return keys.length * 2;
        }
        long size = (long)(keys.length * GROWTH_FACTOR);
        // If new size is larger than MAX_SIZE, clamp it.
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        return (int) size;
    }

    private void expand() {
        if (keys.length == MAX_SIZE) {
            throw new RuntimeException("Map size is too large.");
        }
        SimpleIntMap<T> h = new SimpleIntMap<>(newSize());
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] > MIN_KEY_VALUE) {
                h.put(keys[i], values[i]);
            }
        }
        this.keys = h.keys;
        this.values = h.values;
        this.threshold = h.threshold;
    }
}
