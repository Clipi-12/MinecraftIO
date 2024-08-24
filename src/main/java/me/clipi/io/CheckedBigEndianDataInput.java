package me.clipi.io;

import me.clipi.io.util.function.CheckedByteConsumer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CheckedBigEndianDataInput<ReadException extends Throwable> {
	/**
	 * Similar to {@link OutOfMemoryError}, but it does not extend {@link Error}
	 * so that it has to be explicitly caught.
	 */
	public static final class OomException extends Exception {
		private static final long serialVersionUID = -5526955873890302452L;
	}

	/**
	 * Similar to {@link java.io.UTFDataFormatException}, but it does not extend {@link java.io.IOException}
	 * so that it has to be explicitly caught.
	 */
	public static final class ModifiedUtf8DataFormatException extends Exception {
		private static final long serialVersionUID = 706349407222156161L;
	}

	/**
	 * Similar to {@link java.io.EOFException}, but it does not extend {@link java.io.IOException}
	 * so that it has to be explicitly caught.
	 */
	public static final class EofException extends Exception {
		private static final long serialVersionUID = -541017794358015573L;
	}

	public static final class NotEofException extends Exception {
		private static final long serialVersionUID = 7464819317282595834L;
	}

	private final CheckedReader<ReadException> reader;

	public CheckedBigEndianDataInput(@NotNull CheckedReader<ReadException> reader) {
		this.reader = reader;
	}

	@NotNull
	public static CheckedBigEndianDataInput<IOException> fromIs(@NotNull InputStream is) {
		return new CheckedBigEndianDataInput<>(new CheckedReader<IOException>() {
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
		});
	}

	public <E extends Throwable> void expectedByteFail(byte b, @NotNull CheckedByteConsumer<E> actualByte)
		throws ReadException, EofException, E {
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
		return ((long) longBuffer[0] << 56) | ((long) longBuffer[1] << 48) |
			   ((long) longBuffer[2] << 40) | ((long) longBuffer[3] << 32) |
			   ((long) longBuffer[4] << 24) | (longBuffer[5] << 16) |
			   (longBuffer[6] << 8) | longBuffer[7];
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
		byte[] res;
		try {
			res = new byte[size];
		} catch (OutOfMemoryError err) {
			throw new OomException();
		}
		if (reader.readFullyOrTrue(res)) throw new EofException();
		return res;
	}

	/**
	 * The caller is responsible for asserting that {@code size >= 0}
	 */
	public int @NotNull [] expectIntArray(int size) throws ReadException, EofException, OomException {
		assert size >= 0;
		int[] res;
		try {
			res = new int[size];
		} catch (OutOfMemoryError err) {
			throw new OutOfMemoryError();
		}
		ByteBuffer buf;
		try {
			buf = ByteBuffer.allocate(size << 2).order(ByteOrder.BIG_ENDIAN);
		} catch (OutOfMemoryError err) {
			oomExpectArray(size, true, res);
			return res;
		}

		if (reader.readFullyOrTrue(buf.array())) throw new EofException();
		buf.asIntBuffer().get(res);
		return res;
	}

	/**
	 * The caller is responsible for asserting that {@code size >= 0}
	 */
	public long @NotNull [] expectLongArray(int size) throws ReadException, EofException, OomException {
		assert size >= 0;
		long[] res;
		try {
			res = new long[size];
		} catch (OutOfMemoryError err) {
			throw new OomException();
		}
		ByteBuffer buf;
		try {
			buf = ByteBuffer.allocate(size << 3).order(ByteOrder.BIG_ENDIAN);
		} catch (OutOfMemoryError err) {
			oomExpectArray(size, false, res);
			return res;
		}

		if (reader.readFullyOrTrue(buf.array())) throw new EofException();
		buf.asLongBuffer().get(res);
		return res;
	}

	private void oomExpectArray(long size, boolean isIntArrayOrElseLongArray, @NotNull Object array)
		throws ReadException, EofException {
		System.gc();
		ByteBuffer buf = this.buf8KiB;
		int offset = 0;
		CheckedReader<ReadException> reader = this.reader;
		int byteShiftAmount = isIntArrayOrElseLongArray ? 2 : 3;
		try {
			while (size > 0) {
				int bytes = (int) Math.min(size << byteShiftAmount, KiB8);
				if (reader.readFullyOrTrue(buf.array(), bytes)) throw new EofException();
				int objs = bytes >> byteShiftAmount;
				if (isIntArrayOrElseLongArray) {
					buf.asIntBuffer().get((int[]) array, offset, objs);
				} else {
					buf.asLongBuffer().get((long[]) array, offset, objs);
				}
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
	 * <p>This method does not throw a checked {@link OomException} since the maximum amount of memory it can
	 * allocate is {@code 0.32 MiB}
	 *
	 * @see java.io.DataInput#readUTF()
	 */
	@NotNull
	public String expectModifiedUtf8() throws ReadException, CheckedBigEndianDataInput.EofException,
											  ModifiedUtf8DataFormatException {
		int bytes = expectShort();
		byte[] encoded;
		try {
			encoded = expectByteArray(bytes);
		} catch (OomException ex) {
			throw new RuntimeException(ex);
		}
		char[] decoded = new char[bytes];
		int chars = 0;
		for (int i = 0; i < bytes; ++i) {
			char decodedChar;
			byte a = encoded[i];
			decodedChar:
			if (a >= 0) {
				decodedChar = (char) a;
			} else {
				++i;
				int shifted = (a & 0xFF) >>> 4;
				if (shifted >>> 1 == 0b110) {
					if (i < bytes) {
						int b = encoded[i] & 0xFF;
						if (b >>> 6 == 0b10) {
							decodedChar = (char) (((a & 0x1F) << 6) | (b & 0x3F));
							break decodedChar;
						}
					}
				} else if (shifted == 0b1110) {
					if (i + 1 < bytes) {
						int b = encoded[i] & 0xFF, c = encoded[i + 1] & 0xFF;
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

		return new String(decoded, 0, chars);
	}
}
