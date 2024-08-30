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
import me.clipi.io.nbt.schema.*;
import me.clipi.io.util.GrowableArray;
import me.clipi.io.util.function.CheckedFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.lang.reflect.Array;

public final class SaveCompoundSchema extends AllowAllCompoundSchema {
	static CheckedFunction<@Nullable OomAware, @NotNull NbtCompound, OomException> nbtCompoundConstructor;

	static {
		NbtCompound.clinit();
	}

	public final @NotNull NbtCompound compound;
	public final @NotNull OomAware oomAware;

	@NotNull
	public static SaveCompoundSchema create(@NotNull OomAware oomAware) throws OomException {
		return oomAware.tryRun(() -> new SaveCompoundSchema(oomAware));
	}

	private SaveCompoundSchema(@NotNull OomAware oomAware) throws OomException {
		compound = nbtCompoundConstructor.apply(oomAware);
		this.oomAware = oomAware;
	}

	@Override
	public void toString(@NotNull Nester nester) {
		compound.toString(nester);
	}

	// <editor-fold defaultstate="collapsed" desc="list of objects">
	@Override
	@NotNull
	public NbtListOfByteArraysSchema schemaForListOfByteArrays(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return oomAware.tryRun(() -> new ListOfObjects<>(oomAware, new byte[length][]));
	}

	@Override
	@NotNull
	public NbtListOfIntArraysSchema schemaForListOfIntArrays(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return oomAware.tryRun(() -> new ListOfObjects<>(oomAware, new int[length][]));
	}

	@Override
	@NotNull
	public NbtListOfLongArraysSchema schemaForListOfLongArrays(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return oomAware.tryRun(() -> new ListOfObjects<>(oomAware, new long[length][]));
	}

	@Override
	@NotNull
	public NbtListOfStringsSchema schemaForListOfStrings(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return oomAware.tryRun(() -> new ListOfObjects<>(oomAware, new String[length]));
	}

	@Override
	@NotNull
	public NbtListOfListsSchema schemaForListOfLists(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return oomAware.tryRun(() -> new ListOfObjects<>(oomAware, new NbtList[length]));
	}

	@Override
	@NotNull
	public NbtListOfCompoundsSchema schemaForListOfCompounds(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return oomAware.tryRun(() -> new ListOfCompounds(oomAware, new NbtCompound[length]));
	}
	// </editor-fold>

	@Override
	@NotNull
	public NbtCompoundSchema schemaForCompound(@NotNull String key) throws OomException {
		return SaveCompoundSchema.create(oomAware);
	}

	static final class ListOfCompounds implements NbtListOfCompoundsSchema {
		private final @NotNull OomAware oomAware;
		final @NotNull NbtCompound @NotNull [] array;

		private ListOfCompounds(@NotNull OomAware oomAware, @NotNull NbtCompound @NotNull [] array) {
			this.oomAware = oomAware;
			this.array = array;
		}

		@Override
		@NotNull
		public NbtCompoundSchema schemaForCompound(int index) throws OomException {
			SaveCompoundSchema schema = SaveCompoundSchema.create(oomAware);
			array[index] = schema.compound;
			return schema;
		}
	}

	private static final class ListOfObjects<T> extends AllowAllListOfListsSchema
		implements NbtListOfByteArraysSchema, NbtListOfIntArraysSchema, NbtListOfLongArraysSchema,
				   NbtListOfStringsSchema {
		private final @NotNull OomAware oomAware;

		private ListOfObjects(@NotNull OomAware oomAware, @NotNull T @NotNull [] array) {
			assert Array.getLength(array) > 0;
			this.oomAware = oomAware;
		}

		// <editor-fold defaultstate="collapsed" desc="objects with length (ignore length)">
		@Override
		public boolean deniesByteArray(int index, @Range(from = 0, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
			return false;
		}

		@Override
		public boolean deniesIntArray(int index, @Range(from = 0, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
			return false;
		}

		@Override
		public boolean deniesLongArray(int index, @Range(from = 0, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
			return false;
		}

		@Override
		public boolean deniesString(int index, @Range(from = 0, to = (1 << 16) - 1) int modifiedUtf8ByteLength) {
			return false;
		}
		// </editor-fold>

		// <editor-fold defaultstate="collapsed" desc="objects with length">
		@Override
		public boolean deniesByteArray(int index, byte @NotNull [] value) {
			return false;
		}

		@Override
		public boolean deniesIntArray(int index, int @NotNull [] value) {
			return false;
		}

		@Override
		public boolean deniesLongArray(int index, long @NotNull [] value) {
			return false;
		}

		@Override
		public boolean deniesString(int index, @NotNull String value) {
			return false;
		}
		// </editor-fold>

		@Override
		public boolean deniesEmptyList(int index) {
			return false;
		}

		// <editor-fold defaultstate="collapsed" desc="list of objects with length">
		@Override
		public boolean deniesByteList(int index, byte @NotNull [] value) {
			return false;
		}

		@Override
		public boolean deniesShortList(int index, short @NotNull [] value) {
			return false;
		}

		@Override
		public boolean deniesIntList(int index, int @NotNull [] value) {
			return false;
		}

		@Override
		public boolean deniesLongList(int index, long @NotNull [] value) {
			return false;
		}

		@Override
		public boolean deniesFloatList(int index, float @NotNull [] value) {
			return false;
		}

		@Override
		public boolean deniesDoubleList(int index, double @NotNull [] value) {
			return false;
		}
		// </editor-fold>

		// <editor-fold defaultstate="collapsed" desc="list of lists">
		@NotNull
		public NbtListOfByteArraysSchema schemaForListOfByteArrays(
			int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
			return oomAware.tryRun(() -> new ListOfObjects<>(oomAware, new byte[length][]));
		}

		@NotNull
		public NbtListOfIntArraysSchema schemaForListOfIntArrays(
			int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
			return oomAware.tryRun(() -> new ListOfObjects<>(oomAware, new int[length][]));
		}

		@NotNull
		public NbtListOfLongArraysSchema schemaForListOfLongArrays(
			int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
			return oomAware.tryRun(() -> new ListOfObjects<>(oomAware, new long[length][]));
		}

		@NotNull
		public NbtListOfStringsSchema schemaForListOfStrings(
			int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
			return oomAware.tryRun(() -> new ListOfObjects<>(oomAware, new String[length]));
		}

		@NotNull
		public NbtListOfListsSchema schemaForListOfLists(
			int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
			return oomAware.tryRun(() -> new ListOfObjects<>(oomAware, new NbtList[length]));
		}

		@NotNull
		public NbtListOfCompoundsSchema schemaForListOfCompounds(
			int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
			return oomAware.tryRun(() -> new ListOfCompounds(oomAware, new NbtCompound[length]));
		}
		// </editor-fold>
	}
}
