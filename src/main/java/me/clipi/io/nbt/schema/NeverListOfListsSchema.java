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

public class NeverListOfListsSchema implements NbtListOfListsSchema {
	protected NeverListOfListsSchema() {
	}

	@Override
	public boolean deniesEmptyList(int index) {
		return true;
	}

	@Override
	public boolean deniesByteList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean deniesShortList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean deniesIntList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean deniesLongList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean deniesFloatList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public boolean deniesDoubleList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return true;
	}

	@Override
	public @Nullable NbtListOfByteArraysSchema schemaForListOfByteArrays(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return null;
	}

	@Override
	public @Nullable NbtListOfIntArraysSchema schemaForListOfIntArrays(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return null;
	}

	@Override
	public @Nullable NbtListOfLongArraysSchema schemaForListOfLongArrays(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return null;
	}

	@Override
	public @Nullable NbtListOfStringsSchema schemaForListOfStrings(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return null;
	}

	@Override
	public @Nullable NbtListOfListsSchema schemaForListOfLists(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return null;
	}

	@Override
	public @Nullable NbtListOfCompoundsSchema schemaForListOfCompounds(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
		return null;
	}
}
