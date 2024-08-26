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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class AlwaysListOfListsSchema implements NbtListOfListsSchema {
	protected AlwaysListOfListsSchema() {
	}

	@Override
	public boolean allowsEmptyList(int index) {
		return true;
	}

	@Override
	public boolean allowsByteList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsShortList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsIntList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsLongList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsFloatList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsDoubleList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsByteArrayList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsIntArrayList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsLongArrayList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean allowsStringList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public @Nullable NbtListOfListsSchema schemaForListOfLists(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return NbtListOfListsSchema.ALWAYS;
	}

	@Override
	public @Nullable NbtListOfCompoundsSchema schemaForListOfCompounds(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return NbtListOfCompoundsSchema.ALWAYS;
	}
}
