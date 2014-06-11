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

import java.util.List;
import java.util.Map;

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
	 * @return map from new game state after a random move to probability. The probabilities
	 *   should sum to one.
	 *   For instance if there are 3 possible random moves, leading to states s1, s2, and s3,
	 *   with probability 0.3, 0.4 and 0.3 respectively. the returned Map would be
	 *   { s1 : 0.3 , s2 : 0.4, s3 : 0.3 }
	 * @throws IllegalStateException if current player is not 0
	 */
	Map<GS, Double> nextChanceStatesWithProbs() throws IllegalStateException;

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
