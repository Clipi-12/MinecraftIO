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

import me.clipi.io.OomException;
import me.clipi.io.OomException.OomAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.function.IntFunction;

/**
 * Similar to {@link java.util.ArrayList}, but it allows primitive arrays and may throw a
 * {@link OomException}
 * when growing.
 */
public class GrowableArray<ArrayType extends Cloneable & Serializable> implements OomAware {
	/**
	 * From {@link java.util.ArrayList}'s internal code:
	 * <pre>
	 * The maximum size of array to allocate.
	 * Some VMs reserve some header words in an array.
	 * Attempts to allocate larger arrays may result in
	 * OutOfMemoryError: Requested array size exceeds VM limit
	 * </pre>
	 */
	public static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

	private @Nullable OomAware oomAware;
	private final @NotNull IntFunction<@NotNull ArrayType> gen;
	public @NotNull ArrayType inner;
	private int nextIdx;

	private GrowableArray(@Nullable OomAware oomAware, int initSize,
						  @NotNull IntFunction<@NotNull ArrayType> gen) {
		this.oomAware = oomAware;
		assert initSize > 0;
		inner = gen.apply(initSize);
		this.gen = gen;
	}

	public int getSize() {
		return nextIdx;
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	public void tryShrinkToFit() {
		int size = nextIdx;
		ArrayType inner = this.inner;
		if (Array.getLength(inner) == size) return;

		OomAware oomAware = this.oomAware;
		this.oomAware = null; // Avoid self-calling infinitely
		ArrayType newInner;
		try {
			newInner = OomAware.tryRunOrNull(oomAware, () -> gen.apply(size));
		} finally {
			this.oomAware = oomAware;
		}

		if (newInner == null) return;
		System.arraycopy(inner, 0, newInner, 0, size);
		this.inner = newInner;
	}

	@Override
	public void trySaveFromOom() {
		tryShrinkToFit();
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	private int ensureCapacityFor(int amount) throws OomException {
		assert amount >= 0 & this.nextIdx >= 0;

		final int len = Array.getLength(inner),
			res = this.nextIdx,
			nextIdx = res + amount;
		if (nextIdx < 0 | nextIdx > MAX_ARRAY_SIZE) throw OomException.INSTANCE;
		if (nextIdx > len) {
			int newLen = Math.max(nextIdx, (int) Math.min(Long.highestOneBit(nextIdx - 1) << 1, MAX_ARRAY_SIZE));
			ArrayType newArr = OomAware.tryRunOrNull(oomAware, () -> gen.apply(newLen));
			if (newArr == null) newArr = OomAware.tryRun(null, () -> gen.apply(nextIdx));

			// We shouldn't create a stack variable to reference inner since inner might have been changed to a
			// smaller array in order to fit the new array in ram. By maintaining a stack variable we forbid java from
			// gc-ing inner right before creating the new array.
			System.arraycopy(inner, 0, newArr, 0, len);

			inner = newArr;
		}
		this.nextIdx = nextIdx;
		return res;
	}

	@SuppressWarnings("unchecked")
	public static <T> GrowableArray<T[]> generic(@NotNull Class<T> tClass, @Nullable OomAware oomAware) {
		return new GrowableArray<>(oomAware, 16, size -> (T[]) Array.newInstance(tClass, size));
	}

	// <editor-fold defaultstate="collapsed" desc="create methods">
	public static GrowableArray<byte[]> bytes(@Nullable OomAware oomAware) {
		return new GrowableArray<>(oomAware, 32, byte[]::new);
	}

	public static GrowableArray<short[]> shorts(@Nullable OomAware oomAware) {
		return new GrowableArray<>(oomAware, 16, short[]::new);
	}

	public static GrowableArray<int[]> ints(@Nullable OomAware oomAware) {
		return new GrowableArray<>(oomAware, 8, int[]::new);
	}

	public static GrowableArray<long[]> longs(@Nullable OomAware oomAware) {
		return new GrowableArray<>(oomAware, 4, long[]::new);
	}

	public static GrowableArray<float[]> floats(@Nullable OomAware oomAware) {
		return new GrowableArray<>(oomAware, 8, float[]::new);
	}

	public static GrowableArray<double[]> doubles(@Nullable OomAware oomAware) {
		return new GrowableArray<>(oomAware, 4, double[]::new);
	}
	// </editor-fold>


	public static <T> void add(@NotNull GrowableArray<T[]> self, T item) throws OomException {
		int idx = self.ensureCapacityFor(1);
		self.inner[idx] = item;
	}

	// <editor-fold defaultstate="collapsed" desc="add methods">
	public static void add(@NotNull GrowableArray<byte[]> self, byte item) throws OomException {
		int idx = self.ensureCapacityFor(1);
		self.inner[idx] = item;
	}

	public static void add(@NotNull GrowableArray<short[]> self, short item) throws OomException {
		int idx = self.ensureCapacityFor(1);
		self.inner[idx] = item;
	}

	public static void add(@NotNull GrowableArray<int[]> self, int item) throws OomException {
		int idx = self.ensureCapacityFor(1);
		self.inner[idx] = item;
	}

	public static void add(@NotNull GrowableArray<long[]> self, long item) throws OomException {
		int idx = self.ensureCapacityFor(1);
		self.inner[idx] = item;
	}

	public static void add(@NotNull GrowableArray<float[]> self, float item) throws OomException {
		int idx = self.ensureCapacityFor(1);
		self.inner[idx] = item;
	}

	public static void add(@NotNull GrowableArray<double[]> self, double item) throws OomException {
		int idx = self.ensureCapacityFor(1);
		self.inner[idx] = item;
	}
	// </editor-fold>


	@SafeVarargs
	@SuppressWarnings("varargs")
	public static <T> void addAll(@NotNull GrowableArray<T[]> self, T @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}

	// <editor-fold defaultstate="collapsed" desc="addAll methods">
	public static void addAll(@NotNull GrowableArray<byte[]> self, byte @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}

	public static void addAll(@NotNull GrowableArray<short[]> self, short @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}

	public static void addAll(@NotNull GrowableArray<int[]> self, int @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}

	public static void addAll(@NotNull GrowableArray<long[]> self, long @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}

	public static void addAll(@NotNull GrowableArray<float[]> self, float @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}

	public static void addAll(@NotNull GrowableArray<double[]> self, double @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}
	// </editor-fold>
}
