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
import me.clipi.io.util.GrowableArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public interface NbtListOfListsSchema {
	@NotNull
	NbtListOfListsSchema ALWAYS = new AllowAllListOfListsSchema() {
	};

	boolean deniesEmptyList(int index) throws OomException;


	boolean deniesByteList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesByteList(int index, byte @NotNull [] value) throws OomException;


	boolean deniesShortList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesShortList(int index, short @NotNull [] value) throws OomException;


	boolean deniesIntList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesIntList(int index, int @NotNull [] value) throws OomException;


	boolean deniesLongList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesLongList(int index, long @NotNull [] value) throws OomException;


	boolean deniesFloatList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesFloatList(int index, float @NotNull [] value) throws OomException;


	boolean deniesDoubleList(int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	boolean deniesDoubleList(int index, double @NotNull [] value) throws OomException;


	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfByteArraysSchema schemaForListOfByteArrays(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfIntArraysSchema schemaForListOfIntArrays(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfLongArraysSchema schemaForListOfLongArrays(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfStringsSchema schemaForListOfStrings(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfListsSchema schemaForListOfLists(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;

	/**
	 * @return The schema for the specified list, or {@code null} if the list is not allowed.
	 */
	@Nullable
	NbtListOfCompoundsSchema schemaForListOfCompounds(
		int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException;
}
