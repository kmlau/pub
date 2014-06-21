package com.kmlau.connect4;

public class Move {
	public final int col;

	private Move(int col) {
		this.col = col;
	}

	public String toString() {
		return "Col " + col;
	}

	private static final Move[] allMoves;
	static {
		allMoves = new Move[7];
		for (int i = 0; i < allMoves.length; ++i) allMoves[i] = new Move(i);
	}

	public static Move of(int col) {
		return allMoves[col];
	}
}
