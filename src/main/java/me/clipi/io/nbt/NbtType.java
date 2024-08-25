package me.clipi.io.nbt;

public enum NbtType {
	End, Byte, Short, Int, Long, Float, Double, String, List, Compound, ByteArray, IntArray, LongArray;

	public final int id = ordinal();

	public static NbtType getByIdOrNull(int id) {
		return id >= 0 && id <= LongArray.id ? values()[id] : null;
	}

	/**
	 * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.12.4">
	 * JLS 4.12.4 (<em>constant variables</em>)</a>
	 * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1">
	 * JLS 13.1.3 (<em>constant variable</em> references)</a>
	 */
	static final byte
		tagEnd = 0,
		tagByte = 1,
		tagShort = 2,
		tagInt = 3,
		tagLong = 4,
		tagFloat = 5,
		tagDouble = 6,
		tagByteArray = 7,
		tagString = 8,
		tagList = 9,
		tagCompound = 10,
		tagIntArray = 11,
		tagLongArray = 12;
}
