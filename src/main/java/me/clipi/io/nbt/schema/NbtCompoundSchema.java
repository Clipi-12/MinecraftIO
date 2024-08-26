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

public interface NbtCompoundSchema extends NbtRootSchema {
	boolean allowsSizeToBe(int size);

	boolean allowsByte(@NotNull String key);

	boolean allowsShort(@NotNull String key);

	boolean allowsInt(@NotNull String key);

	boolean allowsLong(@NotNull String key);

	boolean allowsFloat(@NotNull String key);

	boolean allowsDouble(@NotNull String key);

	boolean allowsByteArray(@NotNull String key, int length);

	boolean allowsIntArray(@NotNull String key, int length);

	boolean allowsLongArray(@NotNull String key, int length);

	boolean allowsString(@NotNull String key, @Range(from = 0, to = (1 << 16) - 1) int modifiedUtf8ByteLength);

	boolean allowsEmptyList(@NotNull String key);

	boolean allowsByteList(@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsShortList(@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsIntList(@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsLongList(@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsFloatList(@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsDoubleList(@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsByteArrayList(@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsIntArrayList(@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsLongArrayList(@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsStringList(@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);


	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfListsSchema schemaForListOfLists(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfCompoundsSchema schemaForListOfCompounds(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);
}
