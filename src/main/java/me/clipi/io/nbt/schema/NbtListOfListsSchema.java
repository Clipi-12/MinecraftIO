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

public interface NbtListOfListsSchema {
	@NotNull
	NbtListOfListsSchema NEVER = new NeverListOfListsSchema(), ALWAYS = new AlwaysListOfListsSchema();

	boolean allowsEmptyList(int index);

	boolean allowsByteList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsShortList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsIntList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsLongList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsFloatList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsDoubleList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsByteArrayList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsIntArrayList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsLongArrayList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	boolean allowsStringList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);


	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfListsSchema schemaForListOfLists(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);

	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfCompoundsSchema schemaForListOfCompounds(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length);
}
