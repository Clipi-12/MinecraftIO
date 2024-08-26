package me.clipi.io.util;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class BoxedArrayIterable<T> implements Iterable<T> {
	private final int len;

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
		private final T @NotNull [] array;

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
		private final @NotNull Object array;

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
