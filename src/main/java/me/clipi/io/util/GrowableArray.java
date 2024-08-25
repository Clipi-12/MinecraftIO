package me.clipi.io.util;

import me.clipi.io.OomException;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.function.IntFunction;

/**
 * Similar to {@link java.util.ArrayList}, but it allows primitive arrays and may throw a
 * {@link OomException}
 * when growing.
 */
public class GrowableArray<ArrayType extends Cloneable & Serializable> {
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

	private final @NotNull IntFunction<@NotNull ArrayType> gen;
	public @NotNull ArrayType inner;
	private int nextIdx;

	private GrowableArray(int initSize, @NotNull IntFunction<@NotNull ArrayType> gen) {
		assert initSize > 0;
		inner = gen.apply(initSize);
		this.gen = gen;
	}

	public int getSize() {
		return nextIdx;
	}

	public static GrowableArray<byte[]> bytes() {
		return new GrowableArray<>(32, byte[]::new);
	}

	public static GrowableArray<short[]> shorts() {
		return new GrowableArray<>(16, short[]::new);
	}

	public static GrowableArray<int[]> ints() {
		return new GrowableArray<>(8, int[]::new);
	}

	public static GrowableArray<long[]> longs() {
		return new GrowableArray<>(4, long[]::new);
	}

	public static GrowableArray<float[]> floats() {
		return new GrowableArray<>(8, float[]::new);
	}

	public static GrowableArray<double[]> doubles() {
		return new GrowableArray<>(4, double[]::new);
	}

	@SuppressWarnings("unchecked")
	public static <T> GrowableArray<T[]> generic(@NotNull Class<T> tClass) {
		return new GrowableArray<>(16, size -> (T[]) Array.newInstance(tClass, size));
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	private int ensureCapacityFor(int amount) throws OomException {
		ArrayType arr = this.inner;
		int len = Array.getLength(arr), res = this.nextIdx, nextIdx;
		if ((nextIdx = this.nextIdx += amount) > len) {
			if (nextIdx <= 0) throw new OomException();
			len = Math.min(Integer.highestOneBit(nextIdx) << 1, MAX_ARRAY_SIZE);
			ArrayType newArr;
			oom:
			{
				if (len > 0)
					try {
						newArr = gen.apply(len);
						break oom;
					} catch (OutOfMemoryError ignored) {
					}
				len = nextIdx;
				try {
					newArr = gen.apply(len);
				} catch (OutOfMemoryError err) {
					throw new OomException();
				}
			}
			this.inner = newArr;
			System.arraycopy(arr, 0, newArr, 0, len);
		}
		return res;
	}

	public static <T> void add(@NotNull GrowableArray<T[]> self, T item) throws OomException {
		self.inner[self.ensureCapacityFor(1)] = item;
	}

	public static void add(@NotNull GrowableArray<byte[]> self, byte item) throws OomException {
		self.inner[self.ensureCapacityFor(1)] = item;
	}

	public static void add(@NotNull GrowableArray<short[]> self, short item) throws OomException {
		self.inner[self.ensureCapacityFor(1)] = item;
	}

	public static void add(@NotNull GrowableArray<int[]> self, int item) throws OomException {
		self.inner[self.ensureCapacityFor(1)] = item;
	}

	public static void add(@NotNull GrowableArray<long[]> self, long item) throws OomException {
		self.inner[self.ensureCapacityFor(1)] = item;
	}

	public static void add(@NotNull GrowableArray<float[]> self, float item) throws OomException {
		self.inner[self.ensureCapacityFor(1)] = item;
	}

	public static void add(@NotNull GrowableArray<double[]> self, double item) throws OomException {
		self.inner[self.ensureCapacityFor(1)] = item;
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	public static <T> void addAll(@NotNull GrowableArray<T[]> self, T @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}

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
}
