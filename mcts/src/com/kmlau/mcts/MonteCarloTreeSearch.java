/*
 Copyright (c) 2014 K. M. Lau
 Licensed under the MIT license. You may not use this file unless in compliance with this license.
 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
*/

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
