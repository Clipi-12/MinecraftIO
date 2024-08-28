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

package me.clipi.io.nbt;

import me.clipi.io.OomException;
import me.clipi.io.OomException.OomAware;
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
public class NbtCompound implements NestedToString, OomAware {
	private final @NotNull GrowableArray<@NotNull String[]> keys;
	private final @NotNull GrowableArray<byte[]> types;

	private @Nullable GrowableArray<byte[]> bytes;
	private @Nullable GrowableArray<short[]> shorts;
	private @Nullable GrowableArray<int[]> ints;
	private @Nullable GrowableArray<long[]> longs;
	private @Nullable GrowableArray<float[]> floats;
	private @Nullable GrowableArray<double[]> doubles;
	private @Nullable GrowableArray<@NotNull Object[]> objects;

	private final @NotNull OomAware oomAware;

	/**
	 * package-private
	 */
	NbtCompound(@Nullable OomAware oomAware) throws OomException {
		this.oomAware = oomAware == null ? this : oomAware;
		keys = GrowableArray.generic(String.class, this.oomAware);
		types = GrowableArray.bytes(this.oomAware);
	}

	public int entries() {
		int len = types.getSize();
		assert len == keys.getSize() &
			   len == (bytes == null ? 0 : bytes.getSize()) +
					  (shorts == null ? 0 : shorts.getSize()) +
					  (ints == null ? 0 : ints.getSize()) +
					  (longs == null ? 0 : longs.getSize()) +
					  (floats == null ? 0 : floats.getSize()) +
					  (doubles == null ? 0 : doubles.getSize()) +
					  (objects == null ? 0 : objects.getSize());
		return len;
	}

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
			for (int i = entries() - 1; i >= 0; --i) {
				if (types[i] == NbtType.tagCompound)
					((NbtCompound) objects[count++]).recursivelyShrinkToFit();
			}
		}
	}

	@SuppressWarnings("ConstantValue")
	@Override
	public void trySaveFromOom() {
		// May be true while the object is being constructed
		if (oomAware == null) return;

		recursivelyShrinkToFit();
	}

	// <editor-fold defaultstate="collapsed" desc="add methods">
	private void addKey(@NotNull String key, byte nbtType) throws OomException {
		GrowableArray.add(this.keys, key);
		GrowableArray.add(types, nbtType);
	}

	void collisionUnsafeAddByte(@NotNull String key, byte value) throws OomException {
		addKey(key, NbtType.tagByte);
		GrowableArray.add(bytes == null ? bytes = GrowableArray.bytes(oomAware) : bytes, value);
	}

	void collisionUnsafeAddShort(@NotNull String key, short value) throws OomException {
		addKey(key, NbtType.tagShort);
		GrowableArray.add(shorts == null ? shorts = GrowableArray.shorts(oomAware) : shorts, value);
	}

	void collisionUnsafeAddInt(@NotNull String key, int value) throws OomException {
		addKey(key, NbtType.tagInt);
		GrowableArray.add(ints == null ? ints = GrowableArray.ints(oomAware) : ints, value);
	}

	void collisionUnsafeAddLong(@NotNull String key, long value) throws OomException {
		addKey(key, NbtType.tagLong);
		GrowableArray.add(longs == null ? longs = GrowableArray.longs(oomAware) : longs, value);
	}

	void collisionUnsafeAddFloat(@NotNull String key, float value) throws OomException {
		addKey(key, NbtType.tagFloat);
		GrowableArray.add(floats == null ? floats = GrowableArray.floats(oomAware) : floats, value);
	}

	void collisionUnsafeAddDouble(@NotNull String key, double value) throws OomException {
		addKey(key, NbtType.tagDouble);
		GrowableArray.add(doubles == null ? doubles = GrowableArray.doubles(oomAware) : doubles, value);
	}

	private void collisionUnsafeAddObject(@NotNull String key, @NotNull Object value, byte nbtType) throws OomException {
		addKey(key, nbtType);
		GrowableArray.add(objects == null ? objects = GrowableArray.generic(Object.class, oomAware) : objects, value);
	}

	void collisionUnsafeAddByteArray(@NotNull String key, byte @NotNull [] value) throws OomException {
		collisionUnsafeAddObject(key, value, NbtType.tagByteArray);
	}

	void collisionUnsafeAddIntArray(@NotNull String key, int @NotNull [] value) throws OomException {
		collisionUnsafeAddObject(key, value, NbtType.tagIntArray);
	}

	void collisionUnsafeAddLongArray(@NotNull String key, long @NotNull [] value) throws OomException {
		collisionUnsafeAddObject(key, value, NbtType.tagLongArray);
	}

	void collisionUnsafeAddString(@NotNull String key, @NotNull String value) throws OomException {
		collisionUnsafeAddObject(key, value, NbtType.tagString);
	}

	void collisionUnsafeAddList(@NotNull String key, @NotNull NbtList value) throws OomException {
		collisionUnsafeAddObject(key, value, NbtType.tagList);
	}

	void collisionUnsafeAddCompound(@NotNull String key, @NotNull NbtCompound value) throws OomException {
		// We don't do an exhaustive check for cyclic dependencies since this method is only accessed by the parser
		assert value != this;
		collisionUnsafeAddObject(key, value, NbtType.tagCompound);
	}
	// </editor-fold>


	@Nullable
	public NbtType typeForKey(@NotNull String key) {
		String[] keys = this.keys.inner;
		for (int i = entries() - 1; i >= 0; --i) {
			if (key.equals(keys[i]))
				return NbtType.values()[types.inner[i]];
		}
		return null;
	}

	int indexForKeyWithTypeOrNeg(@NotNull String key, byte nbtType) throws NbtParseException.UnexpectedTagType {
		assert nbtType > 0 & nbtType < 13;

		String[] keys = this.keys.inner;
		byte[] types = this.types.inner;
		int len = entries();

		int count = 0;
		boolean isObj = nbtType >= NbtType.tagByteArray;

		for (int i = 0; i < len; ++i) {
			if (key.equals(keys[i])) {
				if (types[i] != nbtType)
					throw new NbtParseException.UnexpectedTagType(NbtType.values()[nbtType], types[i]);
				return count;
			}
			if (types[i] == nbtType | (isObj && types[i] >= NbtType.tagByteArray)) ++count;
		}
		return -1;
	}

	private int indexForKeyWithTypeOrThrow(@NotNull String key, byte nbtType) throws NbtParseException.UnexpectedTagType, NbtKeyNotFoundException {
		int i = indexForKeyWithTypeOrNeg(key, nbtType);
		if (i < 0) throw new NbtKeyNotFoundException(key, this);
		return i;
	}

	// <editor-fold defaultstate="collapsed" desc="get methods">
	public byte getByteOrThrow(@NotNull String key) throws NbtParseException.UnexpectedTagType,
														   NbtKeyNotFoundException {
		int idx = indexForKeyWithTypeOrThrow(key, NbtType.tagByte);
		assert bytes != null;
		return bytes.inner[idx];
	}

	public short getShortOrThrow(@NotNull String key) throws NbtParseException.UnexpectedTagType,
															 NbtKeyNotFoundException {
		int idx = indexForKeyWithTypeOrThrow(key, NbtType.tagShort);
		assert shorts != null;
		return shorts.inner[idx];
	}

	public int getIntOrThrow(@NotNull String key) throws NbtParseException.UnexpectedTagType,
														 NbtKeyNotFoundException {
		int idx = indexForKeyWithTypeOrThrow(key, NbtType.tagInt);
		assert ints != null;
		return ints.inner[idx];
	}

	public long getLongOrThrow(@NotNull String key) throws NbtParseException.UnexpectedTagType,
														   NbtKeyNotFoundException {
		int idx = indexForKeyWithTypeOrThrow(key, NbtType.tagLong);
		assert longs != null;
		return longs.inner[idx];
	}

	public float getFloatOrThrow(@NotNull String key) throws NbtParseException.UnexpectedTagType,
															 NbtKeyNotFoundException {
		int idx = indexForKeyWithTypeOrThrow(key, NbtType.tagFloat);
		assert floats != null;
		return floats.inner[idx];
	}

	public double getDoubleOrThrow(@NotNull String key) throws NbtParseException.UnexpectedTagType,
															   NbtKeyNotFoundException {
		int idx = indexForKeyWithTypeOrThrow(key, NbtType.tagDouble);
		assert bytes != null;
		return bytes.inner[idx];
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private <T> T getObjectOrNull(@NotNull String key, byte nbtType) throws NbtParseException.UnexpectedTagType {
		int idx = indexForKeyWithTypeOrNeg(key, nbtType);
		assert idx < 0 | objects != null;
		return idx < 0 ? null : (T) objects.inner[idx];
	}

	@NotNull
	private <T> T getObjectOrThrow(@NotNull String key, byte nbtType) throws NbtParseException.UnexpectedTagType,
																			 NbtKeyNotFoundException {
		T res = getObjectOrNull(key, nbtType);
		if (res == null) throw new NbtKeyNotFoundException(key, this);
		return res;
	}

	public byte @Nullable [] getByteArrayOrNull(@NotNull String key) throws NbtParseException.UnexpectedTagType {
		return getObjectOrNull(key, NbtType.tagByteArray);
	}

	public int @Nullable [] getIntArrayOrNull(@NotNull String key) throws NbtParseException.UnexpectedTagType {
		return getObjectOrNull(key, NbtType.tagIntArray);
	}

	public long @Nullable [] getLongArrayOrNull(@NotNull String key) throws NbtParseException.UnexpectedTagType {
		return getObjectOrNull(key, NbtType.tagLongArray);
	}

	public @Nullable String getStringOrNull(@NotNull String key) throws NbtParseException.UnexpectedTagType {
		return getObjectOrNull(key, NbtType.tagString);
	}

	public @Nullable NbtList getListOrNull(@NotNull String key) throws NbtParseException.UnexpectedTagType {
		return getObjectOrNull(key, NbtType.tagList);
	}

	public @Nullable NbtCompound getMapOrNull(@NotNull String key) throws NbtParseException.UnexpectedTagType {
		return getObjectOrNull(key, NbtType.tagCompound);
	}

	public byte @NotNull [] getByteArrayOrThrow(@NotNull String key) throws NbtParseException.UnexpectedTagType,
																			NbtKeyNotFoundException {
		return getObjectOrThrow(key, NbtType.tagByteArray);
	}

	public int @NotNull [] getIntArrayOrThrow(@NotNull String key) throws NbtParseException.UnexpectedTagType,
																		  NbtKeyNotFoundException {
		return getObjectOrThrow(key, NbtType.tagIntArray);
	}

	public long @NotNull [] getLongArrayOrThrow(@NotNull String key) throws NbtParseException.UnexpectedTagType,
																			NbtKeyNotFoundException {
		return getObjectOrThrow(key, NbtType.tagLongArray);
	}

	public @NotNull String getStringOrThrow(@NotNull String key) throws NbtParseException.UnexpectedTagType,
																		NbtKeyNotFoundException {
		return getObjectOrThrow(key, NbtType.tagString);
	}

	public @NotNull NbtList getListOrThrow(@NotNull String key) throws NbtParseException.UnexpectedTagType,
																	   NbtKeyNotFoundException {
		return getObjectOrThrow(key, NbtType.tagList);
	}

	public @NotNull NbtCompound getMapOrThrow(@NotNull String key) throws NbtParseException.UnexpectedTagType,
																		  NbtKeyNotFoundException {
		return getObjectOrThrow(key, NbtType.tagCompound);
	}
	// </editor-fold>


	@Override
	@NotNull
	public String toString() {
		return nestedToString();
	}

	@Override
	public void toString(@NotNull Nester nester) {
		String[] keys = this.keys.inner;
		byte[] types = this.types.inner;

		GrowableArray<?>[] arrays = { null, bytes, shorts, ints, longs, floats, doubles, objects };
		int[] count = new int[8];

		for (int i = 0, len = entries(); i < len; ++i) {
			int type = Math.min(types[i], NbtType.tagByteArray);
			nester.append(keys[i], Array.get(arrays[type].inner, count[type]++));
		}
	}
}
