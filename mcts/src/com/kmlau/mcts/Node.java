package com.kmlau.mcts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A monte carlo tree node, with methods facilitating monte carlo tree search.
 * @author K M Lau
 *
 * @param <Move> The class representing allowed moves by a real non-chance-node player.
 * @param <GS> The game state class
 */
class Node<Move, GS extends GameState<Move, GS>> {
	private static final double UCT_Coef = Math.sqrt(2);

	private final Node<Move, GS> parent;
	final GS gameState;
	final Move causationMove;
	private List<Node<Move, GS>> children;
	private TreeMap<Double, Node<Move, GS>> chanceNodeChildrenByCumulativeProb;

	private int visitCount = 0;
	private double sumScores = 0;

	private final static Random random = new Random();

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

	int visitCount() {
		return visitCount;
	}

	double sumScores() {
		return sumScores;
	}

	List<Node<Move, GS>> getChildren() {
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
			assert chanceNodeChildrenByCumulativeProb == null;
			chanceNodeChildrenByCumulativeProb = new TreeMap<>();
			TreeMap<Double, GS> nextChanceStates = gameState.nextChanceStatesByCumulativeProb();
			for (Map.Entry<Double, GS> entry : nextChanceStates.entrySet()) {
				Node<Move, GS> child = new Node<>(this, entry.getValue(), null);
				children.add(child);
				chanceNodeChildrenByCumulativeProb.put(entry.getKey(), child);
			}
		}
		if (chanceNodeChildrenByCumulativeProb != null) {
			return pickChanceNodeChild(chanceNodeChildrenByCumulativeProb);
		} else {
			return children.isEmpty() ? null : randomElement(children);
		}
	}

	private static <Move, GS extends GameState<Move, GS>> Node<Move, GS> pickChanceNodeChild(
			TreeMap<Double, Node<Move, GS>> chanceNodeChildrenByCumulativeProb) {
		if (chanceNodeChildrenByCumulativeProb.isEmpty()) return null;

		Map.Entry<Double, Node<Move, GS>> chanceChild =
				chanceNodeChildrenByCumulativeProb.higherEntry(random.nextDouble());
		return chanceChild != null ? chanceChild.getValue() : chanceNodeChildrenByCumulativeProb.lastEntry().getValue();

		// GWT compatible code:
		// SortedMap<Double, Node<Move, GS>> x = chanceNodeChildrenByCumulativeProb.tailMap(random.nextDouble() - 1e-7);
		// return x.get(x.firstKey());
	}

	private static <T> T randomElement(List<T> list) {
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
			assert chanceNodeChildrenByCumulativeProb != null;
			return pickChanceNodeChild(chanceNodeChildrenByCumulativeProb);
		} else {
			if (visitCount == 0) {
				return randomElement(children);
			}
			double maxScore = -1;
			Node<Move, GS> selected = null;
			ArrayList<Node<Move, GS>> unvisitedChildren = new ArrayList<>();
			for (Node<Move, GS> child : children) {
				if (child.terminated()) continue;
				if (child.visitCount() == 0) unvisitedChildren.add(child);
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

	double[] simulate(int targetPlayer, double utilityGoal) {
		if (children != null) {
			throw new IllegalStateException("Not a leaf node.");
		}
		GS state = gameState.clone();
		while (!state.terminated()) {
			if (state.currentPlayer() == GameState.PLAYER_CHANCE_NODE) {
				state.makeChanceMove();
			} else {
				Move m = state.suggestedMove();
				assert m != null;
				state.makeMove(m);
			}
			if (state.utility(targetPlayer) > utilityGoal) break;
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

	private Node<Move, GS> findDescendantOfState(GS state, int maxGeneration) {
		if (gameState.equals(state)) return this;
		if ((--maxGeneration) < 0) return null;
		for (Node<Move, GS> child : children) {
			Node<Move, GS> result = child.findDescendantOfState(state, maxGeneration);
			if (result != null) return result;
		}
		return null;
	}
}