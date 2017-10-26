package org.antlr.v4.runtime.dfa.edgemap;

import org.antlr.v4.runtime.dfa.DFAState;

public class DFAEdgeCache {

	private static final int DEFAULT_INITIAL_CAPACITY = 2;

	private static final int DEFAULT_MAX_CAPACITY = 1 << 7;

	// In Java volatile guarantees atomic reference copy.
	// Initial map is the perfect map that expands in case of a collision.
  // If it reaches to a certain size, it is replaced with normal int map that only grows
	// when its size reaches to a certain threshold. This map uses linear probing and
	// expands only size reaches to a certain threshold, until capacity reaches to CAPACITY_LIMIT
	// after that it throws.
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
		while(!edgeMap.put(symbol, state)) {
  		EdgeMap<DFAState> newMap = edgeMap.expand();
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
		EdgeMap<DFAState> map = edgeMap;
		// Obtain a reference, after this even the edgeMap instance is changed by a writer thread,
		// we don't care.
		return map.get(symbol);
	}

	public int size() {
		return edgeMap.size();
	}

	public int capacity() {
		return edgeMap.capacity();
	}

	public int[] getKeys() {return edgeMap.getKeys();}

}
