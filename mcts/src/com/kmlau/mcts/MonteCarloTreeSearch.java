package com.kmlau.mcts;

public class MonteCarloTreeSearch<Move, GS extends GameState<Move, GS>> {

	private Node<Move, GS> selectAndExpand(Node<Move, GS> root) {
		Node<Move, GS> expanded;
		Node<Move, GS> node = root;
		do {
			while (!node.unexpanded()) {
				Node<Move, GS> newNode = node.selectChild();
				if (newNode == null) {
					node.backPropagate(node.gameState.utilities());
					return null;
				}
				node = newNode;
			}
			expanded = node.expand();
		} while (expanded != null && expanded.terminated());
		return expanded;
	}

	public Move searchGoodMove(GS gameState, int timeMillisAllowed, double utilityGoal) {
		if (gameState.currentPlayer() == GameState.PLAYER_CHANCE_NODE) {
			throw new IllegalArgumentException("Game state pertains to a chance node. MCTS cannot compute best move.");
		}
		Node<Move, GS> root = new Node<>(null, gameState, null);
		final long deadline = System.currentTimeMillis() + timeMillisAllowed;

		while (System.currentTimeMillis() < deadline) {
			// Select the best unexpanded node and expand it.
			Node<Move, GS> node = null;
			for (int attempt = 0; node == null && attempt < 4; ++attempt) {
				node = selectAndExpand(root);
			}
			if (node == null) break;

			// Play it out.
			double[] utilities = node.simulate(gameState.currentPlayer(), utilityGoal);
			node.backPropagate(utilities);
		}
		Node<Move, GS> best = null;
		double maxScore = 0;
		for (Node<Move, GS> child : root.getChildren()) {
			if (child.visitCount() > 0) {
				double score = child.sumScores() / child.visitCount();
				if (score > maxScore) {
					maxScore = score;
					best = child;
				}
			}
		}
		return best.causationMove;
	}
}
