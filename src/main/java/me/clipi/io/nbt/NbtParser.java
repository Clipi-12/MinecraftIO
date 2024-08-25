package me.clipi.io.nbt;

import me.clipi.io.CheckedBigEndianDataInput;
import me.clipi.io.nbt.exceptions.*;
import me.clipi.io.util.GrowableArray;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntFunction;

/**
 * @see <a href="https://minecraft.wiki/w/NBT_format">NBT format</a>
 */
public class NbtParser<ReadException extends Throwable> {
	private final CheckedBigEndianDataInput<ReadException> di;

	public NbtParser(CheckedBigEndianDataInput<ReadException> di) {
		this.di = di;
	}

	@NotNull
	private NbtCompound parseRoot()
		throws ReadException, CheckedBigEndianDataInput.EofException, NbtTypeParseException,
			   CheckedBigEndianDataInput.OomException, CheckedBigEndianDataInput.ModifiedUtf8DataFormatException,
			   CheckedBigEndianDataInput.NotEofException, NbtDuplicatedKeyException, NbtListOfVoidException,
			   NbtNegArraySizeException, NbtUnknownTagTypeException {
		di.expectedByteFail(NbtType.tagCompound, type -> {
			throw new NbtTypeParseException(NbtType.Compound, type);
		});
		String key = di.expectModifiedUtf8();
		NbtCompound root = readCompoundValue();
		di.expectEnd();
		NbtCompound res = new NbtCompound();
		res.addMap(key, root);
		return res;
	}

	@SuppressWarnings("StatementWithEmptyBody")
	@NotNull
	private NbtCompound readCompoundValue()
		throws ReadException, CheckedBigEndianDataInput.EofException, CheckedBigEndianDataInput.OomException,
			   CheckedBigEndianDataInput.ModifiedUtf8DataFormatException, NbtDuplicatedKeyException,
			   NbtListOfVoidException, NbtNegArraySizeException, NbtUnknownTagTypeException {
		NbtCompound res = new NbtCompound();
		while (readMapEntry(res)) ;
		return res;
	}

	private boolean readMapEntry(@NotNull NbtCompound target)
		throws ReadException, CheckedBigEndianDataInput.EofException, CheckedBigEndianDataInput.OomException,
			   CheckedBigEndianDataInput.ModifiedUtf8DataFormatException, NbtUnknownTagTypeException,
			   NbtDuplicatedKeyException, NbtNegArraySizeException, NbtListOfVoidException {
		int type = di.expectByte();
		if (type == NbtType.tagEnd) return false;
		String key = di.expectModifiedUtf8();
		switch (type) {
			case NbtType.tagByte:
				target.addByte(key, (byte) di.expectByte());
				break;
			case NbtType.tagShort:
				target.addShort(key, (short) di.expectShort());
				break;
			case NbtType.tagInt:
				target.addInt(key, di.expectInt());
				break;
			case NbtType.tagLong:
				target.addLong(key, di.expectLong());
				break;
			case NbtType.tagFloat:
				target.addFloat(key, di.expectFloat());
				break;
			case NbtType.tagDouble:
				target.addDouble(key, di.expectDouble());
				break;
			case NbtType.tagByteArray:
				target.addByteArray(key, readByteArray());
				break;
			case NbtType.tagIntArray:
				target.addIntArray(key, readIntArray());
				break;
			case NbtType.tagLongArray:
				target.addLongArray(key, readLongArray());
				break;
			case NbtType.tagString:
				target.addString(key, di.expectModifiedUtf8());
				break;
			case NbtType.tagList:
				// TODO Limit stack overflow protection (recursion)
				target.addList(key, readListValue());
				break;
			case NbtType.tagCompound:
				// TODO Limit stack overflow protection (recursion)
				target.addMap(key, readCompoundValue());
				break;
			default:
				throw new NbtUnknownTagTypeException(type);
		}
		return true;
	}

	@NotNull
	private NbtList readListValue()
		throws ReadException, CheckedBigEndianDataInput.EofException, NbtListOfVoidException, NbtNegArraySizeException,
			   CheckedBigEndianDataInput.OomException, NbtUnknownTagTypeException,
			   CheckedBigEndianDataInput.ModifiedUtf8DataFormatException, NbtDuplicatedKeyException {
		int type = di.expectByte();
		int size = di.expectInt();
		if (size == 0) return NbtList.EMPTY_LIST;
		switch (type) {
			case NbtType.tagEnd:
				throw new NbtListOfVoidException();
			case NbtType.tagByte:
				return new NbtList(readByteArray());
			case NbtType.tagShort:
				return new NbtList(readShortArray());
			case NbtType.tagInt:
				return new NbtList(readIntArray());
			case NbtType.tagLong:
				return new NbtList(readLongArray());
			case NbtType.tagFloat:
				return new NbtList(readFloatArray());
			case NbtType.tagDouble:
				return new NbtList(readDoubleArray());
			case NbtType.tagByteArray:
				return new NbtList(readGenericArray(byte[][]::new, this::readByteArray));
			case NbtType.tagIntArray:
				return new NbtList(readGenericArray(int[][]::new, this::readIntArray));
			case NbtType.tagLongArray:
				return new NbtList(readGenericArray(long[][]::new, this::readLongArray));
			case NbtType.tagString:
				return new NbtList(readGenericArray(String[]::new, di::expectModifiedUtf8));
			case NbtType.tagList:
				// TODO Limit stack overflow protection (recursion)
				return new NbtList(readGenericArray(NbtList[]::new, this::readListValue));
			case NbtType.tagCompound:
				// TODO Limit stack overflow protection (recursion)
				return new NbtList(readGenericArray(NbtCompound[]::new, this::readCompoundValue));
			default:
				throw new NbtUnknownTagTypeException(type);
		}
	}

	private int readArraySize() throws ReadException, CheckedBigEndianDataInput.EofException,
									   NbtNegArraySizeException {
		int size = di.expectInt();
		if (size < 0 || size > GrowableArray.MAX_ARRAY_SIZE) throw new NbtNegArraySizeException(size);
		return size;
	}

	private byte[] readByteArray() throws ReadException, CheckedBigEndianDataInput.EofException,
										  NbtNegArraySizeException, CheckedBigEndianDataInput.OomException {
		return di.expectByteArray(readArraySize());
	}

	private short[] readShortArray() throws ReadException, CheckedBigEndianDataInput.EofException,
											NbtNegArraySizeException, CheckedBigEndianDataInput.OomException {
		return di.expectShortArray(readArraySize());
	}

	private int[] readIntArray() throws ReadException, CheckedBigEndianDataInput.EofException,
										NbtNegArraySizeException, CheckedBigEndianDataInput.OomException {
		return di.expectIntArray(readArraySize());
	}

	private long[] readLongArray() throws ReadException, CheckedBigEndianDataInput.EofException,
										  NbtNegArraySizeException, CheckedBigEndianDataInput.OomException {
		return di.expectLongArray(readArraySize());
	}

	private float[] readFloatArray() throws ReadException, CheckedBigEndianDataInput.EofException,
											NbtNegArraySizeException, CheckedBigEndianDataInput.OomException {
		return di.expectFloatArray(readArraySize());
	}

	private double[] readDoubleArray() throws ReadException, CheckedBigEndianDataInput.EofException,
											  NbtNegArraySizeException, CheckedBigEndianDataInput.OomException {
		return di.expectDoubleArray(readArraySize());
	}


	@FunctionalInterface
	private interface ReadGeneric<T, ReadException extends Throwable> {
		@NotNull
		T read() throws ReadException, CheckedBigEndianDataInput.EofException,
						CheckedBigEndianDataInput.OomException,
						CheckedBigEndianDataInput.ModifiedUtf8DataFormatException, NbtNegArraySizeException,
						NbtListOfVoidException, NbtUnknownTagTypeException, NbtDuplicatedKeyException;
	}

	private <T> T[] readGenericArray(@NotNull IntFunction<T @NotNull []> genArray,
									 @NotNull ReadGeneric<T, ReadException> read)
		throws ReadException, CheckedBigEndianDataInput.EofException, NbtNegArraySizeException,
			   CheckedBigEndianDataInput.OomException, CheckedBigEndianDataInput.ModifiedUtf8DataFormatException,
			   NbtListOfVoidException, NbtUnknownTagTypeException, NbtDuplicatedKeyException {
		int len = readArraySize();
		T[] array = genArray.apply(len);
		assert array.length == len;
		for (int i = 0; i < len; ++i)
			array[i] = read.read();
		return array;
	}
}
