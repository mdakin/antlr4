package org.antlr.v4.runtime.dfa.edgemap;

public interface EdgeMap<T> {

	boolean put(int key, T value);

	T get(int key);

	int size();

	int capacity();

	int[] getKeys();

	T[] getValues();

	EdgeMap<T> expand();
}
