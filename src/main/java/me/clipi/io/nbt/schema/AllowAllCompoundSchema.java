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

import me.clipi.io.OomException;
import me.clipi.io.nbt.exceptions.NbtKeyNotFoundException;
import me.clipi.io.nbt.exceptions.NbtParseException;
import me.clipi.io.util.GrowableArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public abstract class AllowAllCompoundSchema implements NbtCompoundSchema {
	protected AllowAllCompoundSchema() {
	}

	@Override
	@NotNull
	public final String toString() {
		return nestedToString();
	}

	@Override
	public boolean deniesFinishedCompound() throws OomException, NbtParseException, NbtKeyNotFoundException {
		return false;
	}

	@Override
	public boolean deniesByte(@NotNull String key, @Range(from = 0, to = (1 << 8) - 1) int value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesShort(@NotNull String key, @Range(from = 0, to = (1 << 16) - 1) int value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesInt(@NotNull String key, int value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesLong(@NotNull String key, long value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesFloat(@NotNull String key, float value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesDouble(@NotNull String key, double value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesByteArray(
		@NotNull String key, @Range(from = 0, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return false;
	}

	@Override
	public boolean deniesByteArray(@NotNull String key, byte @NotNull [] value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesIntArray(@NotNull String key, @Range(from = 0, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return false;
	}

	@Override
	public boolean deniesIntArray(@NotNull String key, int @NotNull [] value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesLongArray(
		@NotNull String key, @Range(from = 0, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return false;
	}

	@Override
	public boolean deniesLongArray(@NotNull String key, long @NotNull [] value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesString(@NotNull String key, @Range(from = 0, to = (1 << 16) - 1) int modifiedUtf8ByteLength) throws OomException {
		return false;
	}

	@Override
	public boolean deniesString(@NotNull String key, @NotNull String value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesEmptyList(@NotNull String key) throws OomException {
		return false;
	}

	@Override
	public boolean deniesByteList(@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return false;
	}

	@Override
	public boolean deniesByteList(@NotNull String key, byte @NotNull [] value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesShortList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return false;
	}

	@Override
	public boolean deniesShortList(@NotNull String key, short @NotNull [] value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesIntList(@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return false;
	}

	@Override
	public boolean deniesIntList(@NotNull String key, int @NotNull [] value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesLongList(@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return false;
	}

	@Override
	public boolean deniesLongList(@NotNull String key, long @NotNull [] value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesFloatList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return false;
	}

	@Override
	public boolean deniesFloatList(@NotNull String key, float @NotNull [] value) throws OomException {
		return false;
	}

	@Override
	public boolean deniesDoubleList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return false;
	}

	@Override
	public boolean deniesDoubleList(@NotNull String key, double @NotNull [] value) throws OomException {
		return false;
	}

	@Override
	public @Nullable NbtListOfByteArraysSchema schemaForListOfByteArrays(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return NbtListOfByteArraysSchema.ALWAYS;
	}

	@Override
	public @Nullable NbtListOfIntArraysSchema schemaForListOfIntArrays(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return NbtListOfIntArraysSchema.ALWAYS;
	}

	@Override
	public @Nullable NbtListOfLongArraysSchema schemaForListOfLongArrays(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return NbtListOfLongArraysSchema.ALWAYS;
	}

	@Override
	public @Nullable NbtListOfStringsSchema schemaForListOfStrings(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return NbtListOfStringsSchema.ALWAYS;
	}

	@Override
	public @Nullable NbtListOfListsSchema schemaForListOfLists(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return NbtListOfListsSchema.ALWAYS;
	}

	@Override
	public @Nullable NbtListOfCompoundsSchema schemaForListOfCompounds(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return NbtListOfCompoundsSchema.ALWAYS;
	}

	@Override
	public @Nullable NbtCompoundSchema schemaForCompound(@NotNull String key) throws OomException {
		return NbtCompoundSchema.ALWAYS;
	}
}
