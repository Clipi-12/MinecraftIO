/*
 * MinecraftIO, a simple library with multiple Minecraft IO-tools
 * Copyright (C) 2024  Clipi (GitHub: Clipi-12)
 *
 * This file is part of MinecraftIO.
 *
 * MinecraftIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MinecraftIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MinecraftIO.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.clipi.io.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Objects;

public final class FixedStack<T> {
	public static final class FullStackException extends Exception {
		private static final long serialVersionUID = 6925719468686425117L;
		public final int attemptedSize;

		public FullStackException(int attemptedSize) {
			this.attemptedSize = attemptedSize;
		}
	}

	public static final class EmptyStackException extends Exception {
		private static final long serialVersionUID = 5571954854914784638L;
	}

	public final int maxSize;
	private final @Nullable T @NotNull [] backingArray;
	private int nextIdx;

	@SuppressWarnings("unchecked")
	public FixedStack(@NotNull Class<T> tClass, int size) {
		this.maxSize = size;
		this.backingArray = (T[]) Array.newInstance(tClass, size);
	}

	public int getSize() {
		return nextIdx;
	}

	public void clear() {
		@SuppressWarnings("UnnecessaryLocalVariable")
		T[] arr = backingArray;
		for (int i = nextIdx - 1; i >= 0; --i)
			arr[i] = null;
		nextIdx = 0;
	}

	public void push(@NotNull T element) throws FullStackException {
		if (!tryPush(element)) throw new FullStackException(backingArray.length + 1);
	}

	/**
	 * The first element of the array will be the last to be popped.
	 *
	 * <p>If the elements don't fit, none will be pushed
	 */
	@SafeVarargs
	public final void pushAll(@NotNull T @NotNull ... elements) throws FullStackException {
		if (!tryPushAll(elements)) throw new FullStackException(backingArray.length + elements.length);
	}

	@NotNull
	@Contract(pure = true)
	public T peek() throws EmptyStackException {
		T res = tryPeek();
		if (res == null) throw new EmptyStackException();
		return res;
	}

	@NotNull
	public T pop() throws EmptyStackException {
		T res = tryPop();
		if (res == null) throw new EmptyStackException();
		return res;
	}

	public boolean tryPush(@NotNull T element) {
		int nextIdx = this.nextIdx;
		if (nextIdx < maxSize) {
			backingArray[nextIdx] = Objects.requireNonNull(element);
			++this.nextIdx;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * The first element of the array will be the last to be popped.
	 *
	 * <p>If the elements don't fit, none will be pushed
	 */
	@SafeVarargs
	public final boolean tryPushAll(@NotNull T @NotNull ... elements) {
		int nextIdx = this.nextIdx + elements.length;
		if (nextIdx <= maxSize) {
			this.nextIdx = nextIdx--;
			@SuppressWarnings("UnnecessaryLocalVariable")
			T[] arr = backingArray;
			// System.arraycopy with null check
			for (int e = elements.length - 1; e >= 0; --e, --nextIdx)
				arr[nextIdx] = Objects.requireNonNull(elements[e]);
			return true;
		} else {
			return false;
		}
	}

	@Nullable
	@Contract(pure = true)
	public T tryPeek() {
		int nextIdx = this.nextIdx;
		if (nextIdx > 0) {
			return backingArray[--nextIdx];
		} else {
			return null;
		}
	}

	@Nullable
	public T tryPop() {
		int nextIdx = this.nextIdx;
		if (nextIdx > 0) {
			T[] arr = backingArray;
			T res = arr[--nextIdx];
			arr[nextIdx] = null;
			this.nextIdx = nextIdx;
			return res;
		} else {
			return null;
		}
	}
}
