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
import me.clipi.io.util.NestedToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

/**
 * Represents a NBT List
 *
 * <p>Empty lists will always be represented by the componentType being the End tag and the backing array being null.
 * The backing array will never be null if the list is not empty.
 */
public final class NbtList implements NestedToString {
	public final @NotNull NbtType componentType;
	public final @Nullable Object array;

	@Override
	@NotNull
	public String toString() {
		return nestedToString();
	}

	@Override
	public void toString(@NotNull Nester nester) {
		nester.append("component type", componentType)
			  .append("size", array == null ? 0 : Array.getLength(array))
			  .append("array", array);
	}

	public static final NbtList EMPTY_LIST = new NbtList();

	private NbtList() {
		// Empty list instance
		this.componentType = NbtType.End;
		this.array = null;
	}

	private NbtList(@NotNull NbtType componentType, @NotNull Object array) {
		assert Array.getLength(array) > 0;
		this.componentType = componentType;
		this.array = array;
	}

	// <editor-fold defaultstate="collapsed" desc="public create methods">
	public static NbtList create(byte @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Byte, array);
	}

	public static NbtList create(short @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Short, array);
	}

	public static NbtList create(int @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Int, array);
	}

	public static NbtList create(long @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Long, array);
	}

	public static NbtList create(float @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Float, array);
	}

	public static NbtList create(double @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Double, array);
	}

	public static NbtList create(byte @NotNull [] @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.ByteArray, array);
	}

	public static NbtList create(int @NotNull [] @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.IntArray, array);
	}

	public static NbtList create(long @NotNull [] @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.LongArray, array);
	}

	public static NbtList create(@NotNull String @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.String, array);
	}

	public static NbtList create(@NotNull NbtList @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.List, array);
	}

	public static NbtList create(@NotNull NbtCompound @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Compound, array);
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="package-private create methods">
	static NbtList emptyUnsafeCreate(@Nullable OomAware oomAware, byte @NotNull [] array) throws OomException {
		return OomAware.tryRun(oomAware, () -> new NbtList(NbtType.Byte, array));
	}

	static NbtList emptyUnsafeCreate(@Nullable OomAware oomAware, short @NotNull [] array) throws OomException {
		return OomAware.tryRun(oomAware, () -> new NbtList(NbtType.Short, array));
	}

	static NbtList emptyUnsafeCreate(@Nullable OomAware oomAware, int @NotNull [] array) throws OomException {
		return OomAware.tryRun(oomAware, () -> new NbtList(NbtType.Int, array));
	}

	static NbtList emptyUnsafeCreate(@Nullable OomAware oomAware, long @NotNull [] array) throws OomException {
		return OomAware.tryRun(oomAware, () -> new NbtList(NbtType.Long, array));
	}

	static NbtList emptyUnsafeCreate(@Nullable OomAware oomAware, float @NotNull [] array) throws OomException {
		return OomAware.tryRun(oomAware, () -> new NbtList(NbtType.Float, array));
	}

	static NbtList emptyUnsafeCreate(@Nullable OomAware oomAware, double @NotNull [] array) throws OomException {
		return OomAware.tryRun(oomAware, () -> new NbtList(NbtType.Double, array));
	}

	static NbtList emptyUnsafeCreate(@Nullable OomAware oomAware, byte @NotNull [] @NotNull [] array) throws OomException {
		return OomAware.tryRun(oomAware, () -> new NbtList(NbtType.ByteArray, array));
	}

	static NbtList emptyUnsafeCreate(@Nullable OomAware oomAware, int @NotNull [] @NotNull [] array) throws OomException {
		return OomAware.tryRun(oomAware, () -> new NbtList(NbtType.IntArray, array));
	}

	static NbtList emptyUnsafeCreate(@Nullable OomAware oomAware, long @NotNull [] @NotNull [] array) throws OomException {
		return OomAware.tryRun(oomAware, () -> new NbtList(NbtType.LongArray, array));
	}

	static NbtList emptyUnsafeCreate(@Nullable OomAware oomAware, @NotNull String @NotNull [] array) throws OomException {
		return OomAware.tryRun(oomAware, () -> new NbtList(NbtType.String, array));
	}

	static NbtList emptyUnsafeCreate(@Nullable OomAware oomAware, @NotNull NbtList @NotNull [] array) throws OomException {
		return OomAware.tryRun(oomAware, () -> new NbtList(NbtType.List, array));
	}

	static NbtList emptyUnsafeCreate(@Nullable OomAware oomAware, @NotNull NbtCompound @NotNull [] array) throws OomException {
		return OomAware.tryRun(oomAware, () -> new NbtList(NbtType.Compound, array));
	}
	// </editor-fold>
}
