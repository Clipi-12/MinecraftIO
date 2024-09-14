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
public final class NbtCompound extends ValuelessNbtCompound implements NestedToString, OomAware {
	final @NotNull GrowableArray<byte[]> types;

	@Nullable
	GrowableArray<byte[]> bytes;
	@Nullable
	GrowableArray<short[]> shorts;
	@Nullable
	GrowableArray<int[]> ints;
	@Nullable
	GrowableArray<long[]> longs;
	@Nullable
	GrowableArray<float[]> floats;
	@Nullable
	GrowableArray<double[]> doubles;
	@Nullable
	GrowableArray<@NotNull Object[]> objects;

	static void clinit() {
		SaveCompoundSchema.nbtCompoundConstructor = NbtCompound::new;
	}

	private NbtCompound(@Nullable OomAware oomAware) throws OomException {
		super(oomAware);
		types = GrowableArray.bytes(this.oomAware);
	}

	public int entries() {
		int len = super.entries();
		assert len == types.getSize() &
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
		super.recursivelyShrinkToFit();
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
			for (int i = 0, count = 0, len = this.objects.getSize(); count < len; ++i) {
				if (types[i] == NbtType.tagCompound)
					((NbtCompound) objects[count++]).recursivelyShrinkToFit();
			}
		}
	}

	@SuppressWarnings("ConstantValue")
	@Override
	public void trySaveFromOom() {
		// May be true while the object is being constructed
		if (types == null) return;

		recursivelyShrinkToFit();
	}

	// <editor-fold defaultstate="collapsed" desc="add methods">
	@Override
	void addKey(@NotNull String key, byte nbtType) throws OomException {
		super.addKey(key, nbtType);
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

	void collisionUnsafeAddObject(@NotNull String key, @NotNull Object value, byte nbtType) throws OomException {
		addKey(key, nbtType);
		GrowableArray.add(objects == null ? objects = GrowableArray.generic(Object.class, oomAware) : objects, value);
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

	private int indexForKeyWithTypeOrNeg(@NotNull String key, byte nbtType) throws NbtParseException.UnexpectedTagType {
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
			byte actualType = types[i];
			if (actualType == nbtType | (isObj & actualType >= NbtType.tagByteArray)) ++count;
		}
		return -1;
	}

	private int indexForKeyWithTypeOrThrow(@NotNull String key, byte nbtType)
		throws NbtParseException.UnexpectedTagType, NbtKeyNotFoundException {
		int i = indexForKeyWithTypeOrNeg(key, nbtType);
		if (i < 0) throw new NbtKeyNotFoundException(oomAware, key, this);
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
		if (res == null) throw new NbtKeyNotFoundException(oomAware, key, this);
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

	@SuppressWarnings("DataFlowIssue")
	public void copyTo(@NotNull NbtCompound dest) throws OomException, NbtParseException.DuplicatedKey {
		// <editor-fold defaultstate="collapsed" desc="code">
		String[] keys = this.keys.inner;
		byte[] types = this.types.inner;

		byte[] bytes = this.bytes == null ? null : this.bytes.inner;
		short[] shorts = this.shorts == null ? null : this.shorts.inner;
		int[] ints = this.ints == null ? null : this.ints.inner;
		long[] longs = this.longs == null ? null : this.longs.inner;
		float[] floats = this.floats == null ? null : this.floats.inner;
		double[] doubles = this.doubles == null ? null : this.doubles.inner;
		Object[] objects = this.objects == null ? null : this.objects.inner;

		int bCount = 0, sCount = 0, iCount = 0, lCount = 0, fCount = 0, dCount = 0, oCount = 0;

		for (int i = 0, len = entries(); i < len; ++i) {
			String key = keys[i];
			if (dest.containsKey(key)) throw new NbtParseException.DuplicatedKey(oomAware, key, dest);
			switch (types[i]) {
				case NbtType.tagByte:
					dest.collisionUnsafeAddByte(key, bytes[bCount++]);
					break;
				case NbtType.tagShort:
					dest.collisionUnsafeAddShort(key, shorts[sCount++]);
					break;
				case NbtType.tagInt:
					dest.collisionUnsafeAddInt(key, ints[iCount++]);
					break;
				case NbtType.tagLong:
					dest.collisionUnsafeAddLong(key, longs[lCount++]);
					break;
				case NbtType.tagFloat:
					dest.collisionUnsafeAddFloat(key, floats[fCount++]);
					break;
				case NbtType.tagDouble:
					dest.collisionUnsafeAddDouble(key, doubles[dCount++]);
					break;
				case NbtType.tagByteArray:
					dest.collisionUnsafeAddByteArray(key, (byte[]) objects[oCount++]);
					break;
				case NbtType.tagIntArray:
					dest.collisionUnsafeAddIntArray(key, (int[]) objects[oCount++]);
					break;
				case NbtType.tagLongArray:
					dest.collisionUnsafeAddLongArray(key, (long[]) objects[oCount++]);
					break;
				case NbtType.tagString:
					dest.collisionUnsafeAddString(key, (String) objects[oCount++]);
					break;
				case NbtType.tagList:
					dest.collisionUnsafeAddList(key, (NbtList) objects[oCount++]);
					break;
				case NbtType.tagCompound:
					dest.collisionUnsafeAddCompound(key, (NbtCompound) objects[oCount++]);
					break;
				default:
					throw new IllegalStateException();
			}
		}
		// </editor-fold>
	}

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
