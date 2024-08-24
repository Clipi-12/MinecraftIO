package me.clipi.io;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CheckedReader<E extends Throwable> {
	/**
	 * Gets a byte.
	 *
	 * @return a byte
	 */
	int nextByteOrNeg() throws E;

	/**
	 * Fills the buffer, or returns true if EOF is reached.
	 */
	default boolean readFullyOrTrue(byte @NotNull [] buf) throws E {
		return readFullyOrTrue(buf, buf.length);
	}

	/**
	 * Fills the start of the buffer with {@code length} bytes, or returns true if EOF is reached.
	 */
	boolean readFullyOrTrue(byte @NotNull [] buf, int length) throws E;

	/**
	 * Creates a composite {@link CheckedReader} by reading all the contents of one reader (including the EOF tag,
	 * i.e., the negative result of {@link #nextByteOrNeg()}) before reading the contents of the following reader.
	 *
	 * <p>The EOF tag being returned is specially useful when used in a {@link me.clipi.io.CheckedBigEndianDataInput},
	 * as its buffers will be able to be reused without the results interfering with one another.
	 */
	@SafeVarargs
	static <E extends Throwable> CheckedReader<E> concat(@NotNull CheckedReader<? extends E> @NotNull ... readers) {
		return new CheckedReader<E>() {
			private int nextIndex = 0;
			private CheckedReader<? extends E> currentReader = null;

			@Override
			public int nextByteOrNeg() throws E {
				CheckedReader<? extends E> reader = readerOrNull();
				if (reader == null) return -1;
				int res = reader.nextByteOrNeg();
				if (res < 0) currentReader = null;
				return res;
			}

			@Override
			public boolean readFullyOrTrue(byte @NotNull [] buf, int length) throws E {
				CheckedReader<? extends E> reader = readerOrNull();
				if (reader == null) return true;
				boolean res = reader.readFullyOrTrue(buf, length);
				if (res) currentReader = null;
				return res;
			}

			@Nullable
			private CheckedReader<? extends E> readerOrNull() {
				CheckedReader<? extends E> currentReader = this.currentReader;
				if (currentReader != null) return currentReader;
				int nextIndex = this.nextIndex;
				if (nextIndex < readers.length) {
					++this.nextIndex;
					return readers[nextIndex];
				} else {
					return null;
				}
			}
		};
	}
}
