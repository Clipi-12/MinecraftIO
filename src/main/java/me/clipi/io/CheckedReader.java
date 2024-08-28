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

package me.clipi.io;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a reader, similar to a {@link InputStream}, but with a set ReadException so that it does not
 * necessarily throw an {@link IOException} on method calls.
 *
 * <p>A {@link CheckedReader} may be backed up by multiple inputs.
 * <p>When an input is consumed (i.e., EOF has been reached), subsequent reads must return EOF tags (i.e. negative
 * numbers for {@link #nextByteOrNeg()}, and {@code true} for {@link #readFullyOrTrue(byte[])}).
 * <p>Once {@link #closeCurrent()} is called, the underlying input is closed and the next underlying input will take
 * its place.
 * <p>Once all underlying inputs have been closed (either because {@link #closeAll()} has been called, or the last
 * underlying input has been closed by {@link #closeCurrent()}), i.e. once {@link #isCompletelyClosed()} returns true,
 * subsequent reads will be undefined behaviour. Calls to {@link #closeCurrent()},{@link #closeAll()} and
 * {@link #isCompletelyClosed()} methods are permitted.
 *
 * @apiNote This interface is <strong>not</strong> thread safe.
 */
public interface CheckedReader<ReadException extends Throwable> {
	/**
	 * Fetches a byte from the underlying input, or a negative number as an EOF tag.
	 * <p>Calls to this method after the first EOF tag must always return EOF tags until the underlying input is
	 * closed, at which point calling this method is undefined behaviour.
	 * <p>If this {@link CheckedReader} is backed up by multiple inputs, calling {@link #closeCurrent()} will cycle to
	 * the next underlying input (if any), so subsequent calls to this method may not return EOF tags, even when the
	 * previous underlying input had already been consumed.
	 */
	int nextByteOrNeg() throws ReadException;

	/**
	 * Fills the buffer, or returns true if EOF is reached.
	 */
	default boolean readFullyOrTrue(byte @NotNull [] buf) throws ReadException {
		return readFullyOrTrue(buf, buf.length);
	}

	/**
	 * Fills the start of the buffer with {@code length} bytes, or returns {@code true} if EOF has been reached.
	 * <p>Once EOF has been reached, subsequent calls to this method will always return {@code true}, unless the
	 * underlying input is closed which would make the call to this method undefined behaviour.
	 */
	boolean readFullyOrTrue(byte @NotNull [] buf, int length) throws ReadException;

	/**
	 * Closes the underlying input.
	 * <p>If this {@link CheckedReader} is backed up by multiple inputs, the current underlying input will be closed
	 * and the next underlying input will take its place.
	 */
	void closeCurrent() throws ReadException;

	/**
	 * Closes the underlying input.
	 * <p>If this {@link CheckedReader} is backed up by multiple inputs, it closes all of the remaining inputs.
	 */
	default void closeAll() throws ReadException {
		closeCurrent();
	}

	/**
	 * Whether all the underlying inputs have been closed, indicating that subsequent reads will be undefined
	 * behaviour.
	 *
	 * <p>Once {@link #closeAll()} has been called, this method must always return {@code true}
	 *
	 * @return Whether all the underlying inputs have been closed.
	 */
	boolean isCompletelyClosed();


	/**
	 * Creates a composite {@link CheckedReader} by reading all the contents of one reader (including the EOF tag,
	 * i.e., the negative result of {@link #nextByteOrNeg()}) before reading the contents of the following reader.
	 *
	 * <p>The EOF tag being returned is specially useful when used in a {@link me.clipi.io.CheckedBigEndianDataInput},
	 * as its buffers will be able to be reused without the results interfering with one another.
	 */
	@SafeVarargs
	static <E extends Throwable> CheckedReader<E> concat(@NotNull CheckedReader<? extends E> @NotNull ... readers) {
		final int len = readers.length;
		return new CheckedReader<E>() {
			private int nextIndex;
			private CheckedReader<? extends E> currentReader;

			@Override
			public int nextByteOrNeg() throws E {
				return readerOrCrash().nextByteOrNeg();
			}

			@Override
			public boolean readFullyOrTrue(byte @NotNull [] buf, int length) throws E {
				return readerOrCrash().readFullyOrTrue(buf, length);
			}

			@NotNull
			private CheckedReader<? extends E> readerOrCrash() {
				CheckedReader<? extends E> currentReader = this.currentReader;
				if (currentReader != null) return currentReader;
				int nextIndex = this.nextIndex;
				if (nextIndex < len) {
					++this.nextIndex;
					return this.currentReader = readers[nextIndex];
				} else {
					throw new IllegalStateException("Attempting to read while fully closed");
				}
			}

			@Override
			public void closeCurrent() throws E {
				CheckedReader<? extends E> reader = currentReader;
				if (reader != null) {
					try {
						reader.closeCurrent();
					} finally {
						if (reader.isCompletelyClosed())
							currentReader = null;
					}
				}
			}

			@Override
			@SuppressWarnings("unchecked")
			public void closeAll() throws E {
				CheckedReader<? extends E> reader = currentReader;
				currentReader = null;
				int i = nextIndex;
				nextIndex = len;

				Throwable thrown = null;
				if (reader != null) {
					try {
						reader.closeAll();
					} catch (Throwable ex) {
						thrown = ex;
					}
				}
				for (; i < len; ++i) {
					try {
						readers[i].closeAll();
					} catch (Throwable ex) {
						if (thrown == null) {
							thrown = ex;
						} else {
							thrown.addSuppressed(ex);
						}
					}
				}
				if (thrown != null) {
					if (thrown instanceof Error) {
						throw (Error) thrown;
					} else if (thrown instanceof RuntimeException) {
						throw (RuntimeException) thrown;
					} else {
						throw (E) thrown;
					}
				}
			}

			@Override
			public boolean isCompletelyClosed() {
				return currentReader == null && nextIndex >= len;
			}
		};
	}

	@NotNull
	static CheckedReader<IOException> fromIs(@NotNull InputStream is) {
		return new CheckedReader<IOException>() {
			private boolean closed;

			@Override
			public int nextByteOrNeg() throws IOException {
				return is.read();
			}

			@Override
			public boolean readFullyOrTrue(byte @NotNull [] buf, int length) throws IOException {
				assert length >= 0 && length <= buf.length;
				int n = 0;
				while (length > 0) {
					int count = is.read(buf, n, length);
					if (count < 0) return true;
					n += count;
					length -= count;
				}
				return false;
			}

			@Override
			public void closeCurrent() throws IOException {
				closed = true;
				is.close();
			}

			@Override
			public boolean isCompletelyClosed() {
				return closed;
			}
		};
	}
}
