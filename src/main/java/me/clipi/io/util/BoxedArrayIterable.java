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

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class BoxedArrayIterable<T> implements Iterable<T> {
	public final int len;

	private BoxedArrayIterable(int len) {
		this.len = len;
	}

	public static BoxedArrayIterable<?> infer(@NotNull Object array) {
		if (!array.getClass().isArray()) throw new IllegalArgumentException();
		return array.getClass().getComponentType().isPrimitive() ?
			new BoxedPrimitiveArrayIterable<>(array) :
			new GenericArrayIterable<>((Object[]) array);
	}

	public static <T> GenericArrayIterable<T> create(T @NotNull [] array) {
		return new GenericArrayIterable<>(array);
	}

	public static BoxedPrimitiveArrayIterable<Byte> create(byte @NotNull [] array) {
		return new BoxedPrimitiveArrayIterable<>(array);
	}

	public static BoxedPrimitiveArrayIterable<Short> create(short @NotNull [] array) {
		return new BoxedPrimitiveArrayIterable<>(array);
	}

	public static BoxedPrimitiveArrayIterable<Integer> create(int @NotNull [] array) {
		return new BoxedPrimitiveArrayIterable<>(array);
	}

	public static BoxedPrimitiveArrayIterable<Long> create(long @NotNull [] array) {
		return new BoxedPrimitiveArrayIterable<>(array);
	}

	public static BoxedPrimitiveArrayIterable<Float> create(float @NotNull [] array) {
		return new BoxedPrimitiveArrayIterable<>(array);
	}

	public static BoxedPrimitiveArrayIterable<Double> create(double @NotNull [] array) {
		return new BoxedPrimitiveArrayIterable<>(array);
	}


	public static final class GenericArrayIterable<T> extends BoxedArrayIterable<T> {
		public final T @NotNull [] array;

		private GenericArrayIterable(T @NotNull [] array) {
			super(array.length);
			this.array = array;
		}

		@NotNull
		@Override
		public Iterator<T> iterator() {
			int len = super.len;
			T[] array = this.array;
			return new Iterator<T>() {
				private int i = 0;

				@Override
				public boolean hasNext() {
					return i < len;
				}

				@Override
				public T next() {
					if (hasNext()) {
						return array[i++];
					} else {
						throw new NoSuchElementException();
					}
				}
			};
		}
	}

	public static final class BoxedPrimitiveArrayIterable<T> extends BoxedArrayIterable<T> {
		public final @NotNull Object array;

		private BoxedPrimitiveArrayIterable(@NotNull Object array) {
			super(Array.getLength(array));
			this.array = array;
		}

		@NotNull
		@Override
		public Iterator<T> iterator() {
			int len = super.len;
			Object array = this.array;
			return new Iterator<T>() {
				private int i = 0;

				@Override
				public boolean hasNext() {
					return i < len;
				}

				@Override
				@SuppressWarnings("unchecked")
				public T next() {
					if (hasNext()) {
						return (T) Array.get(array, i++);
					} else {
						throw new NoSuchElementException();
					}
				}
			};
		}
	}
}
