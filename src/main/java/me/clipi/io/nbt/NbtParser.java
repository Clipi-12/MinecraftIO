package me.clipi.io.nbt;

import me.clipi.io.CheckedBigEndianDataInput;
import org.jetbrains.annotations.NotNull;

/**
 * @see <a href="https://minecraft.wiki/w/NBT_format">NBT format</a>
 */
public class NbtParser<ReadException extends Throwable> {
	public enum Type {
		End, Byte, Short, Int, Long, Float, Double, String, List, Compound, ByteArray, IntArray, LongArray;

		public final int id = ordinal();

		public static Type getByIdOrNull(int id) {
			return id >= 0 && id <= LongArray.id ? values()[id] : null;
		}

		/**
		 * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.12.4">
		 * JLS 4.12.4 (<em>constant variables</em>)</a>
		 * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1">
		 * JLS 13.1.3 (<em>constant variable</em> references)</a>
		 */
		private static final byte
			tagEnd = 0,
			tagByte = 1,
			tagShort = 2,
			tagInt = 3,
			tagLong = 4,
			tagFloat = 5,
			tagDouble = 6,
			tagString = 7,
			tagList = 8,
			tagCompound = 9,
			tagByteArray = 10,
			tagIntArray = 11,
			tagLongArray = 12;
	}

	private final CheckedBigEndianDataInput<ReadException> di;

	public NbtParser(CheckedBigEndianDataInput<ReadException> di) {
		this.di = di;
	}

	public void expectEnd() throws ReadException, CheckedBigEndianDataInput.EofException,
								   CheckedBigEndianDataInput.NotEofException, NbtTypeParseException {
		di.expectedByteFail(Type.tagEnd, type -> {
			throw new NbtTypeParseException(Type.End, type);
		});
		di.expectEnd();
	}

	public int expectByte() throws ReadException, CheckedBigEndianDataInput.EofException, NbtTypeParseException {
		di.expectedByteFail(Type.tagByte, type -> {
			throw new NbtTypeParseException(Type.Byte, type);
		});
		return di.expectByte();
	}

	public int expectShort() throws ReadException, CheckedBigEndianDataInput.EofException, NbtTypeParseException {
		di.expectedByteFail(Type.tagShort, type -> {
			throw new NbtTypeParseException(Type.Short, type);
		});
		return di.expectShort();
	}

	public int expectInt() throws ReadException, CheckedBigEndianDataInput.EofException, NbtTypeParseException {
		di.expectedByteFail(Type.tagInt, type -> {
			throw new NbtTypeParseException(Type.Int, type);
		});
		return di.expectInt();
	}

	public long expectLong() throws ReadException, CheckedBigEndianDataInput.EofException, NbtTypeParseException {
		di.expectedByteFail(Type.tagLong, type -> {
			throw new NbtTypeParseException(Type.Long, type);
		});
		return di.expectLong();
	}

	public float expectFloat() throws ReadException, CheckedBigEndianDataInput.EofException, NbtTypeParseException {
		di.expectedByteFail(Type.tagFloat, type -> {
			throw new NbtTypeParseException(Type.Float, type);
		});
		return di.expectFloat();
	}

	public double expectDouble() throws ReadException, CheckedBigEndianDataInput.EofException, NbtTypeParseException {
		di.expectedByteFail(Type.tagDouble, type -> {
			throw new NbtTypeParseException(Type.Double, type);
		});
		return di.expectDouble();
	}


	@NotNull
	public String expectString() throws ReadException, CheckedBigEndianDataInput.EofException,
										CheckedBigEndianDataInput.ModifiedUtf8DataFormatException,
										NbtTypeParseException {
		di.expectedByteFail(Type.tagString, type -> {
			throw new NbtTypeParseException(Type.String, type);
		});
		return di.expectModifiedUtf8();
	}


	/**
	 * From {@link java.util.ArrayList}'s internal code:
	 * <pre>
	 * The maximum size of array to allocate.
	 * Some VMs reserve some header words in an array.
	 * Attempts to allocate larger arrays may result in
	 * OutOfMemoryError: Requested array size exceeds VM limit
	 * </pre>
	 */
	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

	private int readArraySize() throws ReadException, CheckedBigEndianDataInput.EofException,
									   NbtArraySizeParseException {
		int size = di.expectInt();
		if (size < 0 || size > MAX_ARRAY_SIZE) throw new NbtArraySizeParseException(size);
		return size;
	}

	public byte @NotNull [] expectByteArray() throws ReadException, CheckedBigEndianDataInput.EofException,
													 NbtTypeParseException, NbtArraySizeParseException,
													 CheckedBigEndianDataInput.OomException {
		di.expectedByteFail(Type.tagByteArray, type -> {
			throw new NbtTypeParseException(Type.ByteArray, type);
		});
		return di.expectByteArray(readArraySize());
	}

	public int @NotNull [] expectIntArray() throws ReadException, CheckedBigEndianDataInput.EofException,
												   NbtTypeParseException, NbtArraySizeParseException,
												   CheckedBigEndianDataInput.OomException {
		di.expectedByteFail(Type.tagIntArray, type -> {
			throw new NbtTypeParseException(Type.IntArray, type);
		});
		return di.expectIntArray(readArraySize());
	}

	public long @NotNull [] expectLongArray() throws ReadException, CheckedBigEndianDataInput.EofException,
													 NbtTypeParseException, NbtArraySizeParseException,
													 CheckedBigEndianDataInput.OomException {
		di.expectedByteFail(Type.tagLongArray, type -> {
			throw new NbtTypeParseException(Type.LongArray, type);
		});
		return di.expectLongArray(readArraySize());
	}
}
