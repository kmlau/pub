package com.kmlau.g2048;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import com.kmlau.mcts.GameState;
import com.kmlau.mcts.MonteCarloTreeSearch;

/**
 * Game state of a 2048 game.
 * @author K M Lau
 *
 */
public class G2048State implements GameState<G2048State.Move, G2048State> {

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
	private EnumMap<Move, byte[][]> nextBoardCache;

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
				byte val = m.get(row, col, board);
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

	private void populateNextBoardCache() {
		if (nextBoardCache == null) {
			nextBoardCache = new EnumMap<>(Move.class);
			for (Move m : Move.values()) {
				byte[][] b = attemptMove(m);
				if (b != null) nextBoardCache.put(m, b);
			}
		}
	}

	@Override
	public Move suggestedMove() {
		populateNextBoardCache();
		int maxEmpty = -1;
		Move move = null;
		for (Map.Entry<Move, byte[][]> e : nextBoardCache.entrySet()) {
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
		populateNextBoardCache();
		return new ArrayList<>(nextBoardCache.keySet());
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
	public TreeMap<Double, G2048State> nextChanceStatesByCumulativeProb() {
		if (currentPlayer() != PLAYER_CHANCE_NODE) {
			throw new IllegalStateException("Current state is not a chance node.");
		}
		List<Pos> emptyPos = getEmptyPos();
		TreeMap<Double, G2048State> m = new TreeMap<>();
		if (emptyPos.isEmpty()) return m;

		double cumulativeProb = 0;
		final byte[] newVals = {1, 2};
		final double[] probs = {0.9, 0.1};
		for (Pos p : emptyPos) for (int i = 0; i < newVals.length; ++i) {
			G2048State s = clone();
			s.board[p.row][p.col] = newVals[i];
			s.currentPlayer = 1;
			cumulativeProb += probs[i] / emptyPos.size();
			m.put(cumulativeProb, s);
		}
		return m;
	}

	@Override
	public boolean terminated() {
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
		if (player == 1) {
			int u = 0;
			for (byte[] row : board) for (byte v : row) {
				u += v > 0 ? (1 << v) : 100;  // the added 100 for an empty cell is arbitrary.
			}
			return u / 2048.0;
		}
		return 0;
	}

	public double[] utilities() {
		return new double[]{0, utility(1)};
	}

	@Override
	public void makeMove(Move m) throws IllegalStateException, IllegalArgumentException {
		if (currentPlayer() == PLAYER_CHANCE_NODE) {
			throw new IllegalStateException("Cannot make a player move for chance node state.");
		}
		byte[][] newBoard = nextBoardCache != null ? nextBoardCache.get(m) : attemptMove(m);
		if (newBoard == null) {
			throw new IllegalArgumentException("Cannot make move: " + m);
		}
		nextBoardCache = null;
		board = newBoard;
		currentPlayer = PLAYER_CHANCE_NODE;
	}

	@Override
	public void makeChanceMove() throws IllegalStateException {
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

	public G2048State clone() {
		G2048State c = new G2048State(clone(board));
		c.currentPlayer = currentPlayer();
		if (nextBoardCache != null) {
			c.nextBoardCache = nextBoardCache.clone();
			for (Map.Entry<Move, byte[][]> e : c.nextBoardCache.entrySet()) {
				e.setValue(clone(e.getValue()));
			}
		}
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