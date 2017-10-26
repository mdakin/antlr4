package org.antlr.v4.runtime.dfa.edgemap;

public interface EdgeMap<T> {

	boolean put(int key, T value);

	/**
	 * @return The value {@code T} that is mapped to given {@code key}. or  {@code null} If key does
	 * not exist.
	 * @throws IllegalArgumentException if key is {@code Integer.MIN_INT}
	 */
	T get(int key);

	int size();

	int capacity();

	int[] getKeys();

	T[] getValues();

	EdgeMap<T> expand();
}
