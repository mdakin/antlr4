package org.antlr.v4.runtime.misc;

public interface EdgeCache<T> {

	boolean put(int key, T value);

	T get(int key);

	int size();

	int capacity();

	int[] getKeys();

	EdgeCache<T> expand();
}
