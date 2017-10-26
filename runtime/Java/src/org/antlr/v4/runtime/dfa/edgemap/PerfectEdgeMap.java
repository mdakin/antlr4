package org.antlr.v4.runtime.dfa.edgemap;

import java.util.Arrays;

/**
 * A small map with int keys. Properties:
 * - get is always a single lookup.
 * - put may fail.
 * - Is not thread safe
 */
final class PerfectEdgeMap<T> extends BaseEdgeMap<T> {

  private int maxCapacity;

  /**
   * @param capacity initial internal array size. It must be a positive
   * number. If value is not a power of two, size will be the nearest
   * larger power of two.
   */
  @SuppressWarnings("unchecked")
  public PerfectEdgeMap(int capacity, int maxCapacity) {
    capacity = adjustCapacity(capacity) ;
    this.maxCapacity = adjustCapacity(maxCapacity);
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
   * Try to create a new map with double capacity.
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
