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
import me.clipi.io.nbt.NbtCompound;
import me.clipi.io.nbt.exceptions.NbtKeyNotFoundException;
import me.clipi.io.nbt.exceptions.NbtParseException;
import me.clipi.io.util.GrowableArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public interface NbtCompoundSchema {
	@NotNull
	NbtCompoundSchema ALWAYS = new AlwaysCompoundSchema();

	boolean deniesFinishedCompound(@NotNull NbtCompound compound)
		throws OomException, NbtParseException, NbtKeyNotFoundException;

	boolean deniesByte(@NotNull String key) throws OomException;

	boolean deniesShort(@NotNull String key) throws OomException;

	boolean deniesInt(@NotNull String key) throws OomException;

	boolean deniesLong(@NotNull String key) throws OomException;

	boolean deniesFloat(@NotNull String key) throws OomException;

	boolean deniesDouble(@NotNull String key) throws OomException;

	boolean deniesByteArray(
		@NotNull String key, @Range(from = 0, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesIntArray(
		@NotNull String key, @Range(from = 0, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesLongArray(
		@NotNull String key, @Range(from = 0, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesString(
		@NotNull String key, @Range(from = 0, to = (1 << 16) - 1) int modifiedUtf8ByteLength) throws OomException;

	boolean deniesEmptyList(@NotNull String key) throws OomException;

	boolean deniesByteList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesShortList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesIntList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesLongList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesFloatList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesDoubleList(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfByteArraysSchema schemaForListOfByteArrays(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfIntArraysSchema schemaForListOfIntArrays(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfLongArraysSchema schemaForListOfLongArrays(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfStringsSchema schemaForListOfStrings(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfListsSchema schemaForListOfLists(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfCompoundsSchema schemaForListOfCompounds(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	/**
	 * @return The schema for the specified compound, or {@code null} if the compound is not allowed.
	 */
	@Nullable
	NbtCompoundSchema schemaForCompound(@NotNull String key) throws OomException;
}
