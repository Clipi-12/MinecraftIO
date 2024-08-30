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

import me.clipi.io.OomException.OomAware;
import me.clipi.io.util.function.CheckedByteConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.IntFunction;

public class CheckedBigEndianDataInput<ReadException extends Exception> implements AutoCloseable {
	/**
	 * Similar to {@link java.io.UTFDataFormatException}, but it does not extend {@link java.io.IOException}
	 * so that it has to be explicitly caught.
	 */
	public static final class ModifiedUtf8DataFormatException extends Exception {
		private static final long serialVersionUID = 706349407222156161L;
	}

	private final CheckedReader<ReadException> reader;
	private @Nullable OomAware oomAware;

	public void setOomAware(@Nullable OomAware oomAware) {
		this.oomAware = oomAware;
	}

	public CheckedBigEndianDataInput(@NotNull CheckedReader<ReadException> reader) {
		this.reader = reader;
	}

	@Override
	public void close() throws ReadException {
		reader.closeAll();
	}

	public void closeCurrent() throws ReadException {
		reader.closeCurrent();
	}

	public <E extends Throwable> void expectedByteFail(byte b, @NotNull CheckedByteConsumer<E> actualByte) throws ReadException, EofException, E {
		int res = reader.nextByteOrNeg();
		if (res < 0) throw new EofException();
		if (res != b) actualByte.accept((byte) res);
	}

	public void expectEnd() throws ReadException, NotEofException {
		if (reader.nextByteOrNeg() >= 0) throw new NotEofException();
	}

	public int expectByte() throws ReadException, EofException {
		int res = reader.nextByteOrNeg();
		if (res < 0) throw new EofException();
		return res;
	}

	public int expectShort() throws ReadException, EofException {
		int a = reader.nextByteOrNeg(), b = reader.nextByteOrNeg();
		if ((a | b) < 0) throw new EofException();
		return (a << 8) | b;
	}

	public int expectInt() throws ReadException, EofException {
		int a = reader.nextByteOrNeg(), b = reader.nextByteOrNeg(),
			c = reader.nextByteOrNeg(), d = reader.nextByteOrNeg();
		if ((a | b | c | d) < 0) throw new EofException();
		return (a << 24) | (b << 16) | (c << 8) | d;
	}

	private final byte[] bufLong = new byte[8];
	private static final int KiB8 = 8 * 1024;
	private final ByteBuffer buf8KiB = ByteBuffer.allocate(KiB8).order(ByteOrder.BIG_ENDIAN);

	public long expectLong() throws ReadException, EofException {
		byte[] longBuffer = this.bufLong;
		if (reader.readFullyOrTrue(longBuffer)) throw new EofException();
		return ((longBuffer[0] & 0xFFL) << 56) | ((longBuffer[1] & 0xFFL) << 48) |
			   ((longBuffer[2] & 0xFFL) << 40) | ((longBuffer[3] & 0xFFL) << 32) |
			   ((longBuffer[4] & 0xFFL) << 24) | ((longBuffer[5] & 0xFFL) << 16) |
			   ((longBuffer[6] & 0xFFL) << 8) | (longBuffer[7] & 0xFFL);
	}

	public float expectFloat() throws ReadException, EofException {
		return Float.intBitsToFloat(expectInt());
	}

	public double expectDouble() throws ReadException, EofException {
		return Double.longBitsToDouble(expectLong());
	}

	/**
	 * The caller is responsible for asserting that {@code size >= 0}
	 */
	public byte @NotNull [] expectByteArray(int size) throws ReadException, EofException, OomException {
		assert size >= 0;
		byte[] res = OomAware.tryRun(oomAware, () -> new byte[size]);
		if (reader.readFullyOrTrue(res)) throw new EofException();
		return res;
	}

	/**
	 * The caller is responsible for asserting that {@code size >= 0}
	 */
	public short @NotNull [] expectShortArray(int size) throws ReadException, EofException, OomException {
		return expectArray(size, short[]::new, 1, (buf, off, len, arr) -> buf.asShortBuffer().get(arr, off, len));
	}

	/**
	 * The caller is responsible for asserting that {@code size >= 0}
	 */
	public int @NotNull [] expectIntArray(int size) throws ReadException, EofException, OomException {
		return expectArray(size, int[]::new, 2, (buf, off, len, arr) -> buf.asIntBuffer().get(arr, off, len));
	}

	/**
	 * The caller is responsible for asserting that {@code size >= 0}
	 */
	public long @NotNull [] expectLongArray(int size) throws ReadException, EofException, OomException {
		return expectArray(size, long[]::new, 3, (buf, off, len, arr) -> buf.asLongBuffer().get(arr, off, len));
	}

	/**
	 * The caller is responsible for asserting that {@code size >= 0}
	 */
	public float @NotNull [] expectFloatArray(int size) throws ReadException, EofException, OomException {
		return expectArray(size, float[]::new, 2, (buf, off, len, arr) -> buf.asFloatBuffer().get(arr, off, len));
	}

	/**
	 * The caller is responsible for asserting that {@code size >= 0}
	 */
	public double @NotNull [] expectDoubleArray(int size) throws ReadException, EofException, OomException {
		return expectArray(size, double[]::new, 3, (buf, off, len, arr) -> buf.asDoubleBuffer().get(arr, off, len));
	}


	@FunctionalInterface
	private interface CopyFromByteBuffer<Arr> {
		void fromInto(@NotNull ByteBuffer buf, int offset, int len, @NotNull Arr into);
	}

	/**
	 * The caller is responsible for asserting that {@code size >= 0}
	 */
	@NotNull
	private <Arr> Arr expectArray(int size, @NotNull IntFunction<Arr> gen, int byteShiftAmount,
								  @NotNull CopyFromByteBuffer<Arr> copy)
		throws ReadException, EofException, OomException {
		assert size >= 0;
		Arr res = OomAware.tryRun(oomAware, () -> gen.apply(size));
		ByteBuffer buf = OomAware.tryRunOrNull(oomAware, () -> ByteBuffer.allocate(size << byteShiftAmount)
																		 .order(ByteOrder.BIG_ENDIAN));
		if (buf == null) {
			oomExpectArray(size, byteShiftAmount, res, copy);
			return res;
		}

		if (reader.readFullyOrTrue(buf.array())) throw new EofException();
		copy.fromInto(buf, 0, size, res);
		return res;
	}

	private <Arr> void oomExpectArray(long size, int byteShiftAmount, @NotNull Arr array,
									  @NotNull CopyFromByteBuffer<Arr> copy) throws ReadException, EofException {
		ByteBuffer buf = buf8KiB;
		int offset = 0;
		CheckedReader<ReadException> reader = this.reader;
		try {
			while (size > 0) {
				int bytes = (int) Math.min(size << byteShiftAmount, KiB8);
				if (reader.readFullyOrTrue(buf.array(), bytes)) throw new EofException();
				int objs = bytes >> byteShiftAmount;
				copy.fromInto(buf, offset, objs, array);
				offset += objs;
				size -= objs;
			}
		} finally {
			buf.clear();
		}
	}

	/**
	 * Reads a {@link String} in the
	 * <a href="https://docs.oracle.com/javase/8/docs/api/java/io/DataInput.html#modified-utf-8">modified UTF-8</a>
	 * format
	 *
	 * <p>It is unlikely for this method to throw a checked {@link OomException}, since the maximum amount of
	 * memory it can allocate is {@code 0.32 MiB}
	 *
	 * @see java.io.DataInput#readUTF()
	 */
	@NotNull
	public String expectModifiedUtf8() throws ReadException, EofException, OomException,
											  ModifiedUtf8DataFormatException {
		return expectModifiedUtf8(expectShort());
	}

	/**
	 * Reads a {@link String} in the
	 * <a href="https://docs.oracle.com/javase/8/docs/api/java/io/DataInput.html#modified-utf-8">modified UTF-8</a>
	 * format
	 *
	 * <p>It is unlikely for this method to throw a checked {@link OomException}, since the maximum amount of
	 * memory it can allocate is {@code 0.32 MiB}
	 *
	 * @see java.io.DataInput#readUTF()
	 */
	@NotNull
	public String expectModifiedUtf8(short bytes) throws ReadException, EofException, OomException,
														 ModifiedUtf8DataFormatException {
		return expectModifiedUtf8(bytes & 0xFF_FF);
	}

	@NotNull
	private String expectModifiedUtf8(int bytes) throws ReadException, EofException, OomException,
														ModifiedUtf8DataFormatException {
		byte[] encoded = expectByteArray(bytes);
		char[] decoded = OomAware.tryRun(oomAware, () -> new char[bytes]);
		int chars = 0;
		for (int i = 0; i < bytes; ++i) {
			char decodedChar;
			byte a = encoded[i];
			decodedChar:
			if (a >= 0) {
				decodedChar = (char) a;
			} else {
				if (++i < bytes) {
					int b = encoded[i] & 0xFF;
					int shifted = (a & 0xFF) >>> 4;
					if (shifted >>> 1 == 0b110) {
						if (b >>> 6 == 0b10) {
							decodedChar = (char) (((a & 0x1F) << 6) | (b & 0x3F));
							break decodedChar;
						}
					} else if (shifted == 0b1110 && ++i < bytes) {
						int c = encoded[i] & 0xFF;
						if ((b >>> 6 == 0b10) & (b >>> 6 == 0b10)) {
							decodedChar = (char) (((a & 0x0F) << 12) | ((b & 0x3F) << 6) | (c & 0x3F));
							break decodedChar;
						}
					}
				}
				throw new ModifiedUtf8DataFormatException();
			}
			decoded[chars++] = decodedChar;
		}

		int finalChars = chars;
		return OomAware.tryRun(oomAware, () -> new String(decoded, 0, finalChars));
	}
}
