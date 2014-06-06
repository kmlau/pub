package com.kmlau.mcts;

import java.util.List;
import java.util.TreeMap;

/**
 * Interface describing the common attributes of the state of a turn based multi-player game, possibly
 * with nondeterministic game play elements.
 *
 * @param <Move> class of non random moves.
 * @author K M Lau
 *
 */
public interface GameState<Move, GS extends GameState<Move, GS>> {
	final int PLAYER_CHANCE_NODE = 0;

	/**
	 * @return number of players. should never change during the course of a playout.
	 */
	int playerCount();

	/**
	 * @return numeric ID of the player who has the turn to make a move. The id should be
	 *   among {0, 1, 2, ...., playerCount()} , where 0 is the special ID for the non-player making
	 *   random state change.
	 */
	int currentPlayer();

	/**
	 * @return legal moves the current player can make.
	 * @throws IllegalStateException if current player is 0 (non-player making random move)
	 */
	List<Move> possibleMoves() throws IllegalStateException;

	/**
	 * @return map from cumulative probability to new game state after a random move.
	 *   For instance if there are 3 possible random moves, leading to states s1, s2, and s3,
	 *   with probability 0.3, 0.4 and 0.3 respectively. the returned TreeMap would be
	 *   { 0.3 : s1 , 0.7 : s2 , 1.0 : s3 }
	 * @throws IllegalStateException if current player is not 0
	 */
	TreeMap<Double, GS> nextChanceStatesByCumulativeProb() throws IllegalStateException;

	/**
	 * @return whether the game has terminated.
	 */
	boolean terminated();

	/**
	 * @param player numeric player ID
	 * @return the reward for player at the current game state.
	 */
	double utility(int player);

	/**
	 * @return rewards of all players indexed by numeric player id.
	 */
	double[] utilities();

	/**
	 * Make a move on the game state.
	 * @param m
	 * @throws IllegalStateException if this state pertains to a chance node.
	 * @throws IllegalArgumentException if the move, m, is illegal for this state.
	 */
	void makeMove(Move m) throws IllegalStateException, IllegalArgumentException;

	/**
	 * Make a random move.
	 * @throws IllegalStateException if the current player is a real player (not a chance node)
	 */
	void makeChanceMove() throws IllegalStateException;

	GS clone();

	/**
	 * @return a possibly better move among all legal moves for the current player, if the
	 * heuristic is easy to compute. otherwise, returning an arbitrary legal move is fine.
	 */
	Move suggestedMove() throws IllegalStateException;
}
