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

package com.kmlau.connect4;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.kmlau.mcts.GameStateBase;

/**
 * Game state and game play logics for Connect-4 games.
 *
 * @author K M Lau
 */
public class Connect4State extends GameStateBase<Move, Connect4State> {
	private int currentPlayer = 1;
	private int[][] board;
	private static final Random rand = new Random();

	private final CachedValue<Integer> winner = new CachedValue<Integer>() {
		@Override
		protected Integer compute() {
			int winner = 0;
			for (int r = 0; winner == 0 && r < 6; ++r) {
				winner = new Scanner4(r, 0, 0, 1).scan();
			}
			for (int c = 0; winner == 0 && c < 7; ++c) {
				winner = new Scanner4(0, c, 1, 0).scan();
			}
			for (int c = 0; winner == 0 && c <= 3; ++c) {
				winner = new Scanner4(0, c, 1, 1).scan();
			}
			for (int r = 1; winner == 0 && r <= 2; ++r) {
				winner = new Scanner4(r, 0, 1, 1).scan();
			}
			for (int c = 3; winner == 0 && c < 7; ++c) {
				winner = new Scanner4(0, c, 1, -1).scan();
			}
			for (int r = 1; winner == 0 && r <= 2; ++r) {
				winner = new Scanner4(r, 6, 1, -1).scan();
			}
			return winner;
		}
	};

	private class Scanner4 {
		private final int r0, c0;
		private final int dr, dc;
		Scanner4(int r0, int c0, int dr, int dc) {
			this.r0 = r0; this.c0 = c0;
			this.dr = dr; this.dc = dc;
		}

		int scan() {
			int p = 0;
			int n = 0;
			for (int r = r0, c = c0; 0 <= r && r < 6 && 0 <= c && c < 7; r += dr, c += dc) {
				final int x = board[r][c];
				if (x == 0) {
					n = 0;
					continue;
				}
				if (n == 0) {
					p = x;
					n = 1;
				} else {
					if (p == x) {
						if ((++n) == 4) return p;
					} else {
						p = x;
						n = 1;
					}
				}
			}
			return 0;
		}
	}

	private Connect4State(int[][] board) {
		this.board = board;
	}

	public Connect4State() {
		this(new int[6][7]);
	}

	@Override
	public int playerCount() {
		return 2;
	}

	@Override
	public int currentPlayer() {
		return currentPlayer;
	}

	@Override
	public List<Move> possibleMoves() {
		List<Move> moves = new ArrayList<>();
		if (winner.get() > 0) return moves;
		for (int c = 0; c < 7; ++c) {
			if (board[5][c] == 0) moves.add(Move.of(c));
		}
		return moves;
	}

	@Override
	public Map<Connect4State, Double> nextChanceStatesWithProbs() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean terminated() {
		return possibleMoves().size() == 0 || winner.get() > 0;
	}

	@Override
	public double utility(int player) {
		if (winner.get() > 0) {
			return winner.get() == player ? 1 : -1;
		}
		return 0;
	}

	@Override
	public double[] utilities() {
		return new double[]{0, utility(1), utility(2)};
	}

	@Override
	protected void makeMoveInternal(Move m) throws IllegalArgumentException {
		if (winner.get() > 0) throw new IllegalStateException("game already terminated.");
		for (int r = 0; r < 6; ++r) {
			if (board[r][m.col] == 0) {
				board[r][m.col] = currentPlayer();
				currentPlayer = 3 - currentPlayer;
				return;
			}
		}
		throw new IllegalArgumentException("Invalid move");
	}

	@Override
	protected void makeChanceMoveInternal() {
		throw new UnsupportedOperationException();
	}

	private static int[][] clone(int[][] board) {
		int[][] newBoard = new int[6][7];
		for (int r = 0; r < 6; ++r) for (int c = 0; c < 7; ++c) {
			newBoard[r][c] = board[r][c];
		}
		return newBoard;
	}

	@Override
	protected Connect4State cloneInternal() {
		Connect4State s = new Connect4State(clone(board));
		s.currentPlayer = currentPlayer;
		return s;
	}

	@Override
	public Move suggestedMove() throws IllegalStateException {
		List<Move> moves = possibleMoves();
		switch (moves.size()) {
		case 0:
			return null;
		case 1:
			return moves.get(0);
		default:
			return moves.get(rand.nextInt(moves.size()));
		}
	}

	public int getCell(int row, int col) {
		return board[row][col];
	}

	public String toString() {
		StringBuilder buf = new StringBuilder();
		for (int r = 5; r >= 0; --r) {
			for (int c = 0; c < 7; ++c) {
				buf.append("    ").append(board[r][c]).append(',');
			}
			buf.append('\n');
		}
		return buf.toString();
	}

	/*
	public static void main(String... args) throws IOException {
		Connect4State s = new Connect4State();
		MonteCarloTreeSearch<Move, Connect4State> mcts = new MonteCarloTreeSearch<Move, Connect4State>();
		System.out.println(s);

		BufferedReader rd = new BufferedReader(new InputStreamReader(System.in));
		String l;
		while ((l = rd.readLine()) != null) {
			s.makeMove(Move.of(Integer.parseInt(l)));
			Move m = mcts.searchGoodMove(s, 8000, 1);
			s.makeMove(m);
			System.out.println(s);
		}
	}
	*/
}