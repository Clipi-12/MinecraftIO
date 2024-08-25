package me.clipi.io.nbt;

import me.clipi.io.OomException;
import me.clipi.io.nbt.exceptions.NbtKeyNotFoundException;
import me.clipi.io.nbt.exceptions.NbtParseException;
import me.clipi.io.util.GrowableArray;
import me.clipi.io.util.NestedToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

/**
 * Implementation of a NBT Compound that avoids primitive-boxing
 */
public class NbtCompound implements NestedToString, OomException.OomAware {
	private final @NotNull GrowableArray<@NotNull String[]> keys = GrowableArray.generic(String.class, this);
	private final @NotNull GrowableArray<byte[]> types = GrowableArray.bytes(this);

	private @Nullable GrowableArray<byte[]> bytes;
	private @Nullable GrowableArray<short[]> shorts;
	private @Nullable GrowableArray<int[]> ints;
	private @Nullable GrowableArray<long[]> longs;
	private @Nullable GrowableArray<float[]> floats;
	private @Nullable GrowableArray<double[]> doubles;
	private @Nullable GrowableArray<@NotNull Object[]> objects;

	public void recursivelyShrinkToFit() {
		keys.tryShrinkToFit();
		types.tryShrinkToFit();
		if (bytes != null) bytes.tryShrinkToFit();
		if (shorts != null) shorts.tryShrinkToFit();
		if (ints != null) ints.tryShrinkToFit();
		if (longs != null) longs.tryShrinkToFit();
		if (floats != null) floats.tryShrinkToFit();
		if (doubles != null) doubles.tryShrinkToFit();
		if (objects != null) {
			objects.tryShrinkToFit();
			Object[] objects = this.objects.inner;
			byte[] types = this.types.inner;
			int count = 0;
			for (int i = this.types.getSize() - 1; i >= 0; --i) {
				if (types[i] == NbtType.tagCompound)
					((NbtCompound) objects[count++]).recursivelyShrinkToFit();
			}
		}
	}

	@Override
	public void trySaveFromOom() {
		recursivelyShrinkToFit();
	}

	private void addKey(@NotNull String key, byte nbtType) throws NbtParseException.DuplicatedKey, OomException {
		@NotNull String[] keys = this.keys.inner;
		int len = this.types.getSize();
		assert this.keys.getSize() == len;
		for (--len; len >= 0; --len) {
			if (key.equals(keys[len])) throw new NbtParseException.DuplicatedKey(key, this);
		}
		GrowableArray.add(this.keys, key);
		GrowableArray.add(types, nbtType);
	}

	// <editor-fold defaultstate="collapsed" desc="add methods">
	void addByte(@NotNull String key, byte value) throws NbtParseException.DuplicatedKey, OomException {
		addKey(key, NbtType.tagByte);
		GrowableArray.add(bytes == null ? bytes = GrowableArray.bytes(this) : bytes, value);
	}

	void addShort(@NotNull String key, short value) throws NbtParseException.DuplicatedKey, OomException {
		addKey(key, NbtType.tagShort);
		GrowableArray.add(shorts == null ? shorts = GrowableArray.shorts(this) : shorts, value);
	}

	void addInt(@NotNull String key, int value) throws NbtParseException.DuplicatedKey, OomException {
		addKey(key, NbtType.tagInt);
		GrowableArray.add(ints == null ? ints = GrowableArray.ints(this) : ints, value);
	}

	void addLong(@NotNull String key, long value) throws NbtParseException.DuplicatedKey, OomException {
		addKey(key, NbtType.tagLong);
		GrowableArray.add(longs == null ? longs = GrowableArray.longs(this) : longs, value);
	}

	void addFloat(@NotNull String key, float value) throws NbtParseException.DuplicatedKey, OomException {
		addKey(key, NbtType.tagFloat);
		GrowableArray.add(floats == null ? floats = GrowableArray.floats(this) : floats, value);
	}

	void addDouble(@NotNull String key, double value) throws NbtParseException.DuplicatedKey, OomException {
		addKey(key, NbtType.tagDouble);
		GrowableArray.add(doubles == null ? doubles = GrowableArray.doubles(this) : doubles, value);
	}

	private void addObject(@NotNull String key, @NotNull Object value, byte nbtType) throws NbtParseException.DuplicatedKey, OomException {
		addKey(key, nbtType);
		GrowableArray.add(objects == null ? objects = GrowableArray.generic(Object.class, this) : objects, value);
	}

	void addByteArray(@NotNull String key, byte @NotNull [] value) throws NbtParseException.DuplicatedKey,
																		  OomException {
		addObject(key, value, NbtType.tagByteArray);
	}

	void addIntArray(@NotNull String key, int @NotNull [] value) throws NbtParseException.DuplicatedKey, OomException {
		addObject(key, value, NbtType.tagIntArray);
	}

	void addLongArray(@NotNull String key, long @NotNull [] value) throws NbtParseException.DuplicatedKey,
																		  OomException {
		addObject(key, value, NbtType.tagLongArray);
	}

	void addString(@NotNull String key, @NotNull String value) throws NbtParseException.DuplicatedKey, OomException {
		addObject(key, value, NbtType.tagString);
	}

	void addList(@NotNull String key, @NotNull NbtList value) throws NbtParseException.DuplicatedKey, OomException {
		addObject(key, value, NbtType.tagList);
	}

	void addMap(@NotNull String key, @NotNull NbtCompound value) throws NbtParseException.DuplicatedKey, OomException {
		// We don't do an exhaustive check for cyclic dependencies since this method is only accessed by the parser
		assert value != this;
		addObject(key, value, NbtType.tagCompound);
	}
	// </editor-fold>


	@Nullable
	public NbtType typeForKey(@NotNull String key) {
		String[] keys = this.keys.inner;
		int len = this.types.getSize();
		assert this.keys.getSize() == len;

		for (int i = 0; i < len; ++i) {
			if (key.equals(keys[i])) {
				return NbtType.values()[types.inner[i]];
			}
		}
		return null;
	}

	private int indexForKeyWithType(@NotNull String key, byte nbtType) throws NbtParseException.UnexpectedTagType,
																			  NbtKeyNotFoundException {
		assert nbtType > 0 & nbtType < 13;

		String[] keys = this.keys.inner;
		byte[] types = this.types.inner;
		int len = this.types.getSize();
		assert this.keys.getSize() == len;

		int count = 0;

		for (int i = 0; i < len; ++i) {
			if (key.equals(keys[i])) {
				if (types[i] != nbtType)
					throw new NbtParseException.UnexpectedTagType(NbtType.values()[nbtType], types[i]);
				return count;
			}
			if (types[i] == nbtType) ++count;
		}
		throw new NbtKeyNotFoundException(key, this);
	}

	// <editor-fold defaultstate="collapsed" desc="get methods">
	public byte getByte(@NotNull String key) throws NbtParseException.UnexpectedTagType, NbtKeyNotFoundException {
		assert bytes != null;
		return bytes.inner[indexForKeyWithType(key, NbtType.tagByte)];
	}

	public short getShort(@NotNull String key) throws NbtParseException.UnexpectedTagType, NbtKeyNotFoundException {
		assert shorts != null;
		return shorts.inner[indexForKeyWithType(key, NbtType.tagShort)];
	}

	public int getInt(@NotNull String key) throws NbtParseException.UnexpectedTagType, NbtKeyNotFoundException {
		assert ints != null;
		return ints.inner[indexForKeyWithType(key, NbtType.tagInt)];
	}

	public long getLong(@NotNull String key) throws NbtParseException.UnexpectedTagType, NbtKeyNotFoundException {
		assert longs != null;
		return longs.inner[indexForKeyWithType(key, NbtType.tagLong)];
	}

	public float getFloat(@NotNull String key) throws NbtParseException.UnexpectedTagType, NbtKeyNotFoundException {
		assert floats != null;
		return floats.inner[indexForKeyWithType(key, NbtType.tagFloat)];
	}

	public double getDouble(@NotNull String key) throws NbtParseException.UnexpectedTagType, NbtKeyNotFoundException {
		assert bytes != null;
		return bytes.inner[indexForKeyWithType(key, NbtType.tagDouble)];
	}

	public byte @NotNull [] getByteArray(@NotNull String key) throws NbtParseException.UnexpectedTagType,
																	 NbtKeyNotFoundException {
		assert objects != null;
		return (byte @NotNull []) objects.inner[indexForKeyWithType(key, NbtType.tagByteArray)];
	}

	public int @NotNull [] getIntArray(@NotNull String key) throws NbtParseException.UnexpectedTagType,
																   NbtKeyNotFoundException {
		assert objects != null;
		return (int @NotNull []) objects.inner[indexForKeyWithType(key, NbtType.tagIntArray)];
	}

	public long @NotNull [] getLongArray(@NotNull String key) throws NbtParseException.UnexpectedTagType,
																	 NbtKeyNotFoundException {
		assert objects != null;
		return (long @NotNull []) objects.inner[indexForKeyWithType(key, NbtType.tagLongArray)];
	}

	public @NotNull String getString(@NotNull String key) throws NbtParseException.UnexpectedTagType,
																 NbtKeyNotFoundException {
		assert objects != null;
		return (@NotNull String) objects.inner[indexForKeyWithType(key, NbtType.tagString)];
	}

	public @NotNull NbtList getList(@NotNull String key) throws NbtParseException.UnexpectedTagType,
																NbtKeyNotFoundException {
		assert objects != null;
		return (@NotNull NbtList) objects.inner[indexForKeyWithType(key, NbtType.tagList)];
	}

	public @NotNull NbtCompound getMap(@NotNull String key) throws NbtParseException.UnexpectedTagType,
																   NbtKeyNotFoundException {
		assert objects != null;
		return (@NotNull NbtCompound) objects.inner[indexForKeyWithType(key, NbtType.tagCompound)];
	}
	// </editor-fold>


	@Override
	public String toString() {
		return nestedToString();
	}

	@Override
	public void toString(@NotNull Nester nester) {
		String[] keys = this.keys.inner;
		byte[] types = this.types.inner;
		int len = this.types.getSize();
		assert this.keys.getSize() == len;

		Object[] arrays = { null, bytes, shorts, ints, longs, floats, doubles,
							objects, objects, objects, objects, objects, objects };
		int[] count = new int[13];

		for (int i = 0; i < len; ++i) {
			byte type = types[i];
			nester.append(keys[i], Array.get(arrays[type], count[type]++));
		}
	}
}
