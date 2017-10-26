package org.antlr.v4.runtime.dfa.edgemap;

import org.antlr.v4.runtime.dfa.DFAState;

/**
 * Initial map is the perfect map that expands in case of a collision.
 * If it reaches to a certain size, it is replaced with normal int map that only grows
 * when its size reaches to a certain threshold. This map uses linear probing and
 * expands only size reaches to a certain threshold, until capacity reaches to CAPACITY_LIMIT
 * after that it throws.

 * This is also knows as cheap read-write lock trick (see item #5 in the link)
 * https://www.ibm.com/developerworks/java/library/j-jtp06197/index.html
 */
public class DFAEdgeCache {

	private static final int DEFAULT_INITIAL_CAPACITY = 2;

	private static final int DEFAULT_MAX_CAPACITY = 1 << 7;

	//
	// In Java volatile guarantees atomic reference copy.
	//
	private volatile EdgeMap<DFAState> edgeMap;

	public DFAEdgeCache() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_MAX_CAPACITY);
	}

	public DFAEdgeCache(int initialCapacity) {
		this(initialCapacity, DEFAULT_MAX_CAPACITY);
	}

	public DFAEdgeCache(int initialCapacity, int maxCapacity) {
		edgeMap = new PerfectEdgeMap<>(initialCapacity, maxCapacity);
	}

	public synchronized void addEdge(int symbol, DFAState state) {
		while (!edgeMap.put(symbol, state)) {
			EdgeMap<DFAState> newMap = edgeMap.expand();
			// If we fail to insert even if we expand, we switch to use a (non perfect) hashmap.
			if (edgeMap == newMap) {
				newMap = new SymbolEdgeMap<>(edgeMap);
			}
			// Replace the map with new version.
			edgeMap = newMap;
		}
	}

	public DFAState getState(int symbol) {
		// Obtain a reference to current edge map. Even if the edgeMap instance is changed
		// by a writer thread, we can still read a consistent version of the map.
		EdgeMap<DFAState> map = edgeMap;
		return map.get(symbol);
	}

	public int size() {
		return edgeMap.size();
	}

	public int capacity() {
		return edgeMap.capacity();
	}

	public int[] getKeys() {
		return edgeMap.getKeys();
	}

}
