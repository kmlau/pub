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

package com.kmlau.g2048;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.kmlau.mcts.GameStateBase;
import com.kmlau.mcts.MonteCarloTreeSearch;

/**
 * Game state of a 2048 game.
 * @author K M Lau
 *
 */
public class G2048State extends GameStateBase<G2048State.Move, G2048State> {

	private interface BoardAccesser {
		byte get(int row, int col, byte board[][]);
		void set(int row, int col, byte value, byte board[][]);
	}

	public enum Move implements BoardAccesser {
		LEFT {
			public byte get(int row, int col, byte board[][]) {
				return board[row][col];
			}
			public void set(int row, int col, byte value, byte board[][]) {
				board[row][col] = value;
			}
		},
		UP {
			public byte get(int row, int col, byte board[][]) {
				return board[col][row];
			}
			public void set(int row, int col, byte value, byte board[][]) {
				board[col][row] = value;
			}
		},
		RIGHT {
			public byte get(int row, int col, byte board[][]) {
				return board[row][3 - col];
			}
			public void set(int row, int col, byte value, byte board[][]) {
				board[row][3 - col] = value;
			}
		},
		DOWN {
			public byte get(int row, int col, byte board[][]) {
				return board[3 - col][row];
			}
			public void set(int row, int col, byte value, byte board[][]) {
				board[3 - col][row] = value;
			}
		};
	}

	private byte board[][];
	private int currentPlayer = 1;
	private int pastMoveCount = 0;

	private final CachedValue<EnumMap<Move, byte[][]>> nextBoards = new CachedValue<EnumMap<Move, byte[][]>>() {
		@Override
		protected EnumMap<Move, byte[][]> compute() {
			EnumMap<Move, byte[][]> nextBoards = new EnumMap<>(Move.class);
			for (Move m : Move.values()) {
				byte[][] b = attemptMove(m);
				if (b != null) nextBoards.put(m, b);
			}
			return nextBoards;
		}

		protected void copy(Object src) {
			// Deep copy of next-board map.
			@SuppressWarnings("unchecked")
			EnumMap<Move, byte[][]> m = ((EnumMap<Move, byte[][]>) src).clone();
			for (Map.Entry<Move, byte[][]> e : m.entrySet()) {
				e.setValue(G2048State.clone(e.getValue()));
			}
			super.copy(m);
		}
	};

	private final CachedValue<Boolean> gameTerminated = new CachedValue<Boolean>() {
		@Override
		protected Boolean compute() {
			return computeTerminatedness();
		}
	};

	private G2048State(byte board[][]) {
		this.board = board;
	}

	@Override
	public int playerCount() {
		return 1;
	}

	@Override
	public int currentPlayer() {
		return currentPlayer;
	}

	private byte[][] attemptMove(Move m) {
		byte[][] newBoard = new byte[4][4];
		boolean changed = false;
		for (int row = 0; row < 4; ++row) {
			int newcol = 0;
			boolean canCombine = true;
			for (int col = 0; col < 4; ++col) {
				final byte val = m.get(row, col, board);
				if (val > 0) {
					if (newcol > 0 && m.get(row, newcol - 1, newBoard) == val) {
						if (canCombine) {
							m.set(row, newcol - 1, (byte)(val + 1), newBoard);
							changed = true;
						} else {
							m.set(row, newcol++, val, newBoard);
						}
						canCombine = !canCombine;
					} else {
						if (col != newcol) changed = true;
						m.set(row, newcol++, val, newBoard);
						canCombine = true;
					}
				}
			}
		}
		return changed ? newBoard : null;
	}
	

	@Override
	public Move suggestedMove() {
		int maxEmpty = -1;
		Move move = null;
		for (Map.Entry<Move, byte[][]> e : nextBoards.get().entrySet()) {
			int count = countEmpty(e.getValue());
			if (count > maxEmpty) {
				maxEmpty = count;
				move = e.getKey();
			}
		}
		return move;
	}

	@Override
	public List<Move> possibleMoves() {
		if (currentPlayer() == PLAYER_CHANCE_NODE) {
			throw new IllegalStateException("Current state is a chance node.");
		}
		return new ArrayList<>(nextBoards.get().keySet());
	}

	private static class Pos {
		final int row, col;
		Pos(int row, int col) {
			this.row = row;
			this.col = col;
		}
	}

	private List<Pos> getEmptyPos() {
		List<Pos> emptyPos = new ArrayList<>();
		for (int row = 0; row < 4; ++row) for (int col = 0; col < 4; ++col) {
			if (board[row][col] == 0) emptyPos.add(new Pos(row, col));
		}
		return emptyPos;
	}

	@Override
	public Map<G2048State, Double> nextChanceStatesWithProbs() {
		if (currentPlayer() != PLAYER_CHANCE_NODE) {
			throw new IllegalStateException("Current state is not a chance node.");
		}
		List<Pos> emptyPos = getEmptyPos();
		Map<G2048State, Double> m = new HashMap<>();
		if (emptyPos.isEmpty()) return m;

		final byte[] newVals = {1, 2};
		final double[] probs = {0.9, 0.1};
		for (Pos p : emptyPos) for (int i = 0; i < newVals.length; ++i) {
			G2048State s = clone();
			s.board[p.row][p.col] = newVals[i];
			s.currentPlayer = 1;
			m.put(s, probs[i] / emptyPos.size());
		}
		return m;
	}

	@Override
	public boolean terminated() {
		return gameTerminated.get();
	}

	private boolean computeTerminatedness() {
		boolean noEmptyCell = true;
		for (int i = 0; i < 4 && noEmptyCell; ++i) for (int j = 0; j < 4; ++j) {
			if (board[i][j] == 0) {
				noEmptyCell = false;
				break;
			}
		}
		if (currentPlayer() == PLAYER_CHANCE_NODE) {
			return noEmptyCell;
		}
		if (!noEmptyCell) return false;
		for (int i = 0; i < 4; ++i) {
			byte prev1 = board[i][0];
			byte prev2 = board[0][i];
			for (int j = 1; j < 4; ++j) {
				byte v1 = board[i][j];
				if (prev1 == v1) return false;
				prev1 = v1;

				byte v2 = board[j][i];
				if (prev2 == v2) return false;
				prev2 = v2;
			}
		}
		return true;
	}

	@Override
	public double utility(int player) {
		return player == 1 ? pastMoveCount / 2048.0 : 0;
	}

	public double[] utilities() {
		return new double[]{0, utility(1)};
	}

	@Override
	protected void makeMoveInternal(Move m) throws IllegalStateException, IllegalArgumentException {
		if (currentPlayer() == PLAYER_CHANCE_NODE) {
			throw new IllegalStateException("Cannot make a player move for chance node state.");
		}
		byte[][] newBoard = nextBoards.populated() ? nextBoards.get().get(m) : attemptMove(m);
		if (newBoard == null) {
			throw new IllegalArgumentException("Cannot make move: " + m);
		}
		board = newBoard;
		currentPlayer = PLAYER_CHANCE_NODE;
		++pastMoveCount;
	}

	@Override
	protected void makeChanceMoveInternal() throws IllegalStateException {
		if (currentPlayer() != PLAYER_CHANCE_NODE) {
			throw new IllegalStateException("Not a chance node state.");
		}

		List<Pos> emptyPos = getEmptyPos();
		if (emptyPos.isEmpty()) {
			throw new IllegalStateException("Game terminated.");
		}
		Random r = new Random();
		Pos p = emptyPos.get(r.nextInt(emptyPos.size()));
		board[p.row][p.col] = r.nextDouble() < 0.9 ? (byte)1 : 2;
		currentPlayer = 1;
		++pastMoveCount;
	}

	private static int countEmpty(byte[][] board) {
		int count = 0;
		for (byte[] row : board) for (byte v : row) {
			if (v == 0) ++count;
		}
		return count;
	}

	private static byte[][] clone(byte[][] board) {
		byte[][] newBoard = new byte[4][4];
		for (int i = 0; i < 4; ++i) for (int j = 0; j < 4; ++j) newBoard[i][j] = board[i][j];
		return newBoard;
	}

	protected G2048State cloneInternal() {
		G2048State c = new G2048State(clone(board));
		c.currentPlayer = currentPlayer;
		c.pastMoveCount = pastMoveCount;
		return c;
	}

	public int getTile(int row, int col) {
		byte b = board[row][col];
		return b > 0 ? 1 << b : 0;
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		for (int row = 0; row < 4; ++row) {
			for (int col = 0; col < 4; ++col) {
				int v = getTile(row, col);
				if (v > 0) {
					if (v < 10000) b.append(' ');
					if (v < 1000) b.append(' ');
					if (v < 100) b.append(' ');
					if (v < 10) b.append(' ');
					b.append(v).append(',');
				} else {
					b.append("     ,");
				}
			}
			b.append('\n');
		}
		return b.toString();
	}

	public boolean equals(Object x) {
		if (x instanceof G2048State) {
			G2048State that = (G2048State) x;
			if (currentPlayer() == that.currentPlayer()) {
				for (int i = 0; i < 4; ++i) {
					if (!Arrays.equals(board[i], that.board[i])) return false;
				}
				return true;
			}
		}
		return false;
	}

	public static class Builder {
		private final byte[][] board = new byte[4][4];

		public Builder setBoard(int row, int col, int val) {
			board[row][col] = (byte)val;
			return this;
		}

		public G2048State build() {
			return new G2048State(board);
		}
	}

	public static void main(String[] args) {
		G2048State state = new Builder().setBoard(2, 1, 1).setBoard(3, 2, 1).build();
		System.out.println(state);
		MonteCarloTreeSearch<G2048State.Move, G2048State> mcts = new MonteCarloTreeSearch<G2048State.Move, G2048State>();
		while (true) {
			G2048State.Move m = mcts.searchGoodMove(state, 800, 2.5);
			state.makeMove(m);
			state.makeChanceMove();
			System.out.println(m);
			System.out.println(state);
		}
	}
}