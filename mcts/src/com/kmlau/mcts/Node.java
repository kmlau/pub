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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A monte carlo tree node, with methods facilitating monte carlo tree search.
 * @author K M Lau
 *
 * @param <Move> The class representing allowed moves by a real non-chance-node player.
 * @param <GS> The game state class
 */
public class Node<Move, GS extends GameState<Move, GS>> {
	private static final double UCT_Coef = Math.sqrt(2);

	private final Node<Move, GS> parent;
	private final GS gameState;
	private final Move causationMove;
	private List<Node<Move, GS>> children;
	private WeightedRandom<Node<Move, GS>> chanceNodeChildren;

	private int visitCount = 0;
	private double sumScores = 0;

	private static final Random random = new Random();

	Node(Node<Move, GS> parent, GS gameState, Move move) {
		this.parent = parent;
		this.gameState = gameState;
		this.causationMove = move;
	}

	boolean unexpanded() {
		return children == null;
	}

	boolean terminated() {
		return children != null && children.isEmpty();
	}

	public int visitCount() {
		return visitCount;
	}

	public double sumScores() {
		return sumScores;
	}

	public Move causationMove() {
		return causationMove;
	}

	public GS gameState() {
		return gameState;
	}

	public List<Node<Move, GS>> getChildren() {
		return children;
	}

	Node<Move, GS> expand() {
		if (children != null) {
			throw new IllegalStateException("Node has already been expanded.");
		}
		children = new ArrayList<>();
		if (gameState.currentPlayer() != GameState.PLAYER_CHANCE_NODE) {
			List<Move> possibleMoves = gameState.possibleMoves();
			for (Move m : possibleMoves) {
				GS nextState = gameState.clone();
				nextState.makeMove(m);
				children.add(new Node<>(this, nextState, m));
			}
		} else {
			assert chanceNodeChildren == null;
			Map<Node<Move, GS>, Double> chanceNodeChildrenWithProb = new HashMap<>();
			for (Map.Entry<GS, Double> entry : gameState.nextChanceStatesWithProbs().entrySet()) {
				Node<Move, GS> child = new Node<>(this, entry.getKey(), null);
				children.add(child);
				chanceNodeChildrenWithProb.put(child, entry.getValue());
			}
			chanceNodeChildren = new WeightedRandom<>(chanceNodeChildrenWithProb, random);
		}
		if (chanceNodeChildren != null) {
			return chanceNodeChildren.get();
		} else {
			return randomElement(children);
		}
	}

	private <T> T randomElement(List<T> list) {
		final int s = list.size();
		switch (s) {
		case 0:
			return null;
		case 1:
			return list.get(0);
		default:
			return list.get(random.nextInt(list.size()));
		}
	}

	Node<Move, GS> selectChild() {
		if (unexpanded() || terminated()) {
			// Leaf node or node of a terminated state, nothing to select.
			return null;
		}
		if (gameState.currentPlayer() == GameState.PLAYER_CHANCE_NODE) {
			assert chanceNodeChildren != null;
			return chanceNodeChildren.get();
		} else {
			if (visitCount == 0) {
				return randomElement(children);
			}
			double maxScore = -Double.MAX_VALUE;
			Node<Move, GS> selected = null;
			ArrayList<Node<Move, GS>> unvisitedChildren = new ArrayList<>();
			for (Node<Move, GS> child : children) {
				if (child.visitCount() == 0) {
					unvisitedChildren.add(child);
					continue;
				}
				if (unvisitedChildren.isEmpty()) {
					double uctScore = child.sumScores() / child.visitCount() +
						UCT_Coef * Math.sqrt(Math.log(visitCount) / child.visitCount());
					if (uctScore > maxScore) {
						maxScore = uctScore;
						selected = child;
					}
				}
			}
			return unvisitedChildren.isEmpty() ? selected : randomElement(unvisitedChildren);
		}
	}

	double[] simulate() {
		if (children != null && !children.isEmpty()) {
			throw new IllegalStateException("Not a leaf node: " + gameState + "; child count: " + children.size());
		}

		GS state = gameState.clone();
		while (!state.terminated()) {
			if (state.currentPlayer() == GameState.PLAYER_CHANCE_NODE) {
				state.makeChanceMove();
			} else {
				Move m = randomElement(state.possibleMoves());
				assert m != null;
				state.makeMove(m);
			}
		}
		return state.utilities();
	}

	void backPropagate(double[] utilities) {
		Node<Move, GS> node = this;
		while (node.parent != null) {
			++node.visitCount;
			node.sumScores += utilities[node.parent.gameState.currentPlayer()];
			node = node.parent;
		}
	}
}