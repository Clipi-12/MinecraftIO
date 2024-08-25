package me.clipi.io.util;

import me.clipi.io.OomException;
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

	@SuppressWarnings("SuspiciousSystemArraycopy")
	public void tryShrinkToFit() {
		int size = nextIdx;
		ArrayType inner = this.inner;
		if (Array.getLength(inner) == size) return;
		ArrayType newInner;
		try {
			newInner = gen.apply(size);
		} catch (OutOfMemoryError err) {
			return;
		}
		System.arraycopy(inner, 0, newInner, 0, size);
		this.inner = newInner;
	}

	@SuppressWarnings("SuspiciousSystemArraycopy")
	private int ensureCapacityFor(int amount, @Nullable Runnable trySaveFromOom) throws OomException {
		ArrayType arr = this.inner;
		int len = Array.getLength(arr), res = this.nextIdx, nextIdx;
		if ((nextIdx = this.nextIdx += amount) > len) {
			if (nextIdx <= 0 | nextIdx > MAX_ARRAY_SIZE) throw new OomException();
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
					break oom;
				} catch (OutOfMemoryError err) {
					if (trySaveFromOom == null) throw new OomException();
					trySaveFromOom.run();
				}
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

	@SuppressWarnings("unchecked")
	public static <T> GrowableArray<T[]> generic(@NotNull Class<T> tClass) {
		return new GrowableArray<>(16, size -> (T[]) Array.newInstance(tClass, size));
	}

	// <editor-fold defaultstate="collapsed" desc="create methods">
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
	// </editor-fold>


	public static <T> void add(@NotNull GrowableArray<T[]> self, T item, @Nullable Runnable trySaveFromOom) throws OomException {
		self.inner[self.ensureCapacityFor(1, trySaveFromOom)] = item;
	}

	// <editor-fold defaultstate="collapsed" desc="add methods">
	public static void add(@NotNull GrowableArray<byte[]> self, byte item, @Nullable Runnable trySaveFromOom) throws OomException {
		self.inner[self.ensureCapacityFor(1, trySaveFromOom)] = item;
	}

	public static void add(@NotNull GrowableArray<short[]> self, short item, @Nullable Runnable trySaveFromOom) throws OomException {
		self.inner[self.ensureCapacityFor(1, trySaveFromOom)] = item;
	}

	public static void add(@NotNull GrowableArray<int[]> self, int item, @Nullable Runnable trySaveFromOom) throws OomException {
		self.inner[self.ensureCapacityFor(1, trySaveFromOom)] = item;
	}

	public static void add(@NotNull GrowableArray<long[]> self, long item, @Nullable Runnable trySaveFromOom) throws OomException {
		self.inner[self.ensureCapacityFor(1, trySaveFromOom)] = item;
	}

	public static void add(@NotNull GrowableArray<float[]> self, float item, @Nullable Runnable trySaveFromOom) throws OomException {
		self.inner[self.ensureCapacityFor(1, trySaveFromOom)] = item;
	}

	public static void add(@NotNull GrowableArray<double[]> self, double item, @Nullable Runnable trySaveFromOom) throws OomException {
		self.inner[self.ensureCapacityFor(1, trySaveFromOom)] = item;
	}
	// </editor-fold>


	@SafeVarargs
	@SuppressWarnings("varargs")
	public static <T> void addAll(@NotNull GrowableArray<T[]> self, @Nullable Runnable trySaveFromOom,
								  T @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length, trySaveFromOom);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}

	// <editor-fold defaultstate="collapsed" desc="addAll methods">
	public static void addAll(@NotNull GrowableArray<byte[]> self, @Nullable Runnable trySaveFromOom,
							  byte @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length, trySaveFromOom);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}

	public static void addAll(@NotNull GrowableArray<short[]> self, @Nullable Runnable trySaveFromOom,
							  short @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length, trySaveFromOom);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}

	public static void addAll(@NotNull GrowableArray<int[]> self, @Nullable Runnable trySaveFromOom,
							  int @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length, trySaveFromOom);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}

	public static void addAll(@NotNull GrowableArray<long[]> self, @Nullable Runnable trySaveFromOom,
							  long @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length, trySaveFromOom);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}

	public static void addAll(@NotNull GrowableArray<float[]> self, @Nullable Runnable trySaveFromOom,
							  float @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length, trySaveFromOom);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}

	public static void addAll(@NotNull GrowableArray<double[]> self, @Nullable Runnable trySaveFromOom,
							  double @NotNull ... items) throws OomException {
		int idx = self.ensureCapacityFor(items.length, trySaveFromOom);
		System.arraycopy(items, 0, self.inner, idx, items.length);
	}
	// </editor-fold>
}
