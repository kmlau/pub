package com.kmlau.mcts;

import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Randomly picking an instance from a list with specific per instance weights.
 * @author K M Lau
 *
 * @param <V> Class of objects to be randomized.
 */
class WeightedRandom<V> {
	private final TreeMap<Double, V> byCumulativeProb = new TreeMap<>();
	private final Random random;

	WeightedRandom(Map<V, Double> objectToProb, Random random) {
		this.random = random;
		double cumulativeProb = 0;
		for (Map.Entry<V, Double> e : objectToProb.entrySet()) {
			if (e.getValue() > 0) {
				cumulativeProb += e.getValue();
				byCumulativeProb.put(cumulativeProb, e.getKey());
			}
		}
	}

	public V get() {
		if (byCumulativeProb.isEmpty()) return null;

		Map.Entry<Double, V> randomEntry =
				byCumulativeProb.higherEntry(random.nextDouble());
		return randomEntry != null ? randomEntry.getValue() : byCumulativeProb.lastEntry().getValue();

		// GWT compatible code:
		//SortedMap<Double, V> x = byCumulativeProb.tailMap(random.nextDouble() - 1e-7);
		//return x.entrySet().iterator().next().getValue();
	}

}
