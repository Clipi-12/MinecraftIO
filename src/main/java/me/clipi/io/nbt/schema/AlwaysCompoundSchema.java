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

package me.clipi.io.nbt.schema;

import me.clipi.io.util.GrowableArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class AlwaysCompoundSchema implements NbtCompoundSchema {
	protected AlwaysCompoundSchema() {
	}

	@Override
	public boolean allowsSizeToBe(int size) {
		return true;
	}

	@Override
	public boolean allowsByte(@NotNull String key) {
		return true;
	}

	@Override
	public boolean allowsShort(@NotNull String key) {
		return true;
	}

	@Override
	public boolean allowsInt(@NotNull String key) {
		return true;
	}

	@Override
	public boolean allowsLong(@NotNull String key) {
		return true;
	}

	@Override
	public boolean allowsFloat(@NotNull String key) {
		return true;
	}

	@Override
	public boolean allowsDouble(@NotNull String key) {
		return true;
	}

	@Override
	public boolean allowsByteArray(@NotNull String key, int length) {
		return true;
	}

	@Override
	public boolean allowsIntArray(@NotNull String key, int length) {
		return true;
	}

	@Override
	public boolean allowsLongArray(@NotNull String key, int length) {
		return true;
	}

	@Override
	public boolean allowsString(@NotNull String key, @Range(from = 0, to = (1 << 16) - 1) int modifiedUtf8ByteLength) {
		return true;
	}

	@Override
	public boolean allowsEmptyList(@NotNull String key) {
		return true;
	}

	@Override
	public boolean allowsByteList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsShortList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsIntList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsLongList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsFloatList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsDoubleList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsByteArrayList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsIntArrayList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsLongArrayList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsStringList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public @Nullable NbtListOfListsSchema schemaForListOfLists(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return NbtListOfListsSchema.ALWAYS;
	}

	@Override
	public @Nullable NbtListOfCompoundsSchema schemaForListOfCompounds(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return NbtListOfCompoundsSchema.ALWAYS;
	}

	@Override
	public @Nullable NbtCompoundSchema schemaForCompound(@NotNull String key) {
		return NbtCompoundSchema.ALWAYS;
	}
}
