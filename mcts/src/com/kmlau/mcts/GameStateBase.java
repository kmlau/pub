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

/**
 * Base class of a GameState providing chores of managing cache-able computations.
 * Subclass can declare member variables of type CachedValue<T> which will save computed values for repeated
 * use.
 * This base class also implements GameState.clone(), GameState.makeMove(m) and GameState.makeChanceMove() which
 * properly handle cached values for state transitions and state cloning.
 *
 * @author K M Lau
 *
 */
public abstract class GameStateBase<Move, GS extends GameStateBase<Move, GS>> implements GameState<Move, GS> {
	protected abstract class CachedValue<T> {
		private T val;

		protected CachedValue() {
			cachedValues.add(this);
		}

		protected abstract T compute();

		public T get() {
			if (val == null) val = compute();
			return val;
		}

		public boolean populated() {
			return val != null;
		}

		/**
		 * If src is not immutable, Subclass needs to override for deep copying src.
		 * @param src
		 */
		protected void copy(Object src) {
			val = (T) src;
		}

		private void clear() {
			val = null;
		}
	}

	private final ArrayList<CachedValue<?>> cachedValues = new ArrayList<>();

	/**
	 * Subclass to implement logic to clone this, minus dealing with any CachedValue<T> instance variables.
	 * @return a clone of this
	 */
	protected abstract GS cloneInternal();

	/**
	 * Subclass to implement the logic to apply a move to the game state, minus dealing with any
	 * CachedValue<T> instance variables.
	 *
	 * @param m a move to apply to the game state.
	 */
	protected abstract void makeMoveInternal(Move m) throws IllegalStateException, IllegalArgumentException;

	/**
	 * Subclass to implement the logic to make a chance move to the game state, minus dealing with any
	 * CachedValue<T> instance variables.
	 *
	 * @throws IllegalStateException
	 */
	protected abstract void makeChanceMoveInternal() throws IllegalStateException;

	private void clearCachedValues() {
		for (CachedValue<?> v : cachedValues) {
			v.clear();
		}
	}

	public final GS clone() {
		GameStateBase<Move, GS> s = cloneInternal();
		// Copy populated cachedValues.
		for (int i = 0; i < s.cachedValues.size(); ++i) {
			final Object v = cachedValues.get(i).val;
			if (v != null) s.cachedValues.get(i).copy(v);
		}
		return (GS) s;
	}

	public final void makeChanceMove() throws IllegalStateException {
		makeChanceMoveInternal();
		clearCachedValues();
	}

	public final void makeMove(Move m) throws IllegalStateException, IllegalArgumentException {
		makeMoveInternal(m);
		clearCachedValues();
	}
}
