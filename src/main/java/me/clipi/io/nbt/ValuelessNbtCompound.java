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
import me.clipi.io.util.GrowableArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class ValuelessNbtCompound implements OomAware {
	final @NotNull GrowableArray<@NotNull String[]> keys;

	@NotNull
	protected final OomAware oomAware;

	/**
	 * package-private
	 */
	@NotNull
	static ValuelessNbtCompound create(@Nullable OomAware oomAware) throws OomException {
		return OomAware.tryRun(oomAware, () -> {
			try {
				return new Impl(oomAware);
			} catch (OomException ex) {
				throw new OutOfMemoryError();
			}
		});
	}

	private static final class Impl extends ValuelessNbtCompound {
		Impl(@Nullable OomAware oomAware) throws OomException {
			super(oomAware);
		}
	}

	ValuelessNbtCompound(@Nullable OomAware oomAware) throws OomException {
		this.oomAware = oomAware == null ? this : oomAware;
		keys = GrowableArray.generic(String.class, this.oomAware);
	}

	public int entries() {
		return keys.getSize();
	}

	public void recursivelyShrinkToFit() {
		keys.tryShrinkToFit();
	}

	@SuppressWarnings("ConstantValue")
	@Override
	public void trySaveFromOom() {
		// May be true while the object is being constructed
		if (keys == null) return;

		recursivelyShrinkToFit();
	}

	// <editor-fold defaultstate="collapsed" desc="add methods">
	void addKey(@NotNull String key, byte nbtType) throws OomException {
		GrowableArray.add(keys, key);
	}

	void collisionUnsafeAddByte(@NotNull String key, byte value) throws OomException {
		addKey(key, NbtType.tagByte);
	}

	void collisionUnsafeAddShort(@NotNull String key, short value) throws OomException {
		addKey(key, NbtType.tagShort);
	}

	void collisionUnsafeAddInt(@NotNull String key, int value) throws OomException {
		addKey(key, NbtType.tagInt);
	}

	void collisionUnsafeAddLong(@NotNull String key, long value) throws OomException {
		addKey(key, NbtType.tagLong);
	}

	void collisionUnsafeAddFloat(@NotNull String key, float value) throws OomException {
		addKey(key, NbtType.tagFloat);
	}

	void collisionUnsafeAddDouble(@NotNull String key, double value) throws OomException {
		addKey(key, NbtType.tagDouble);
	}

	void collisionUnsafeAddObject(@NotNull String key, @NotNull Object value, byte nbtType) throws OomException {
		addKey(key, nbtType);
	}

	final void collisionUnsafeAddByteArray(@NotNull String key, byte @NotNull [] value) throws OomException {
		collisionUnsafeAddObject(key, value, NbtType.tagByteArray);
	}

	final void collisionUnsafeAddIntArray(@NotNull String key, int @NotNull [] value) throws OomException {
		collisionUnsafeAddObject(key, value, NbtType.tagIntArray);
	}

	final void collisionUnsafeAddLongArray(@NotNull String key, long @NotNull [] value) throws OomException {
		collisionUnsafeAddObject(key, value, NbtType.tagLongArray);
	}

	final void collisionUnsafeAddString(@NotNull String key, @NotNull String value) throws OomException {
		collisionUnsafeAddObject(key, value, NbtType.tagString);
	}

	final void collisionUnsafeAddList(@NotNull String key, @NotNull NbtList value) throws OomException {
		collisionUnsafeAddObject(key, value, NbtType.tagList);
	}

	final void collisionUnsafeAddCompound(@NotNull String key, @NotNull ValuelessNbtCompound value) throws OomException {
		// We don't do an exhaustive check for cyclic dependencies since this method is only accessed by the parser
		assert value != this;
		assert value.getClass() == getClass();
		collisionUnsafeAddObject(key, value, NbtType.tagCompound);
	}
	// </editor-fold>

	public final boolean containsKey(@NotNull String key) {
		String[] keys = this.keys.inner;
		for (int i = entries() - 1; i >= 0; --i) {
			if (key.equals(keys[i]))
				return true;
		}
		return false;
	}
}
