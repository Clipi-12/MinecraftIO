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
import me.clipi.io.OomException.OomAware;
import me.clipi.io.util.GrowableArray;
import me.clipi.io.util.NestedToString;
import me.clipi.io.util.function.CheckedFunction;
import me.clipi.io.util.function.ObjIntCheckedFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.lang.reflect.Array;

public interface NbtListOfCompoundsSchema {
	@NotNull
	NbtListOfCompoundsSchema ALWAYS = index -> NbtCompoundSchema.ALWAYS;

	/**
	 * @return The schema for the specified compound, or {@code null} if the compound is not allowed.
	 */
	@Nullable
	NbtCompoundSchema schemaForCompound(int index) throws OomException;

	final class SchemaList<T extends NbtCompoundSchema> implements NbtListOfCompoundsSchema, NestedToString {
		/**
		 * All schemas.
		 *
		 * <p>None of the elements will be null once the list has been parsed.
		 */
		public final T @NotNull [] schemas;
		private final @NotNull OomAware oomAware;
		private final ObjIntCheckedFunction<OomAware, T, OomException> generateSchema;

		/**
		 * @param length must be the exact length of the expected list
		 */
		@NotNull
		public static <T extends NbtCompoundSchema> SchemaList<T> create(
			@NotNull OomAware oomAware,
			@Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length,
			@NotNull Class<T> tClass, @NotNull CheckedFunction<OomAware, T, OomException> generateSchema) throws OomException {
			return create(oomAware, length, tClass, (o, value) -> generateSchema.apply(o));
		}

		/**
		 * @param length must be the exact length of the expected list
		 */
		@NotNull
		public static <T extends NbtCompoundSchema> SchemaList<T> create(
			@NotNull OomAware oomAware,
			@Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length,
			@NotNull Class<T> tClass, @NotNull ObjIntCheckedFunction<OomAware, T, OomException> generateSchema) throws OomException {
			return oomAware.tryRun(() -> new SchemaList<>(oomAware, length, tClass, generateSchema));
		}

		@SuppressWarnings("unchecked")
		private SchemaList(@NotNull OomAware oomAware, int length, Class<T> tClass,
						   ObjIntCheckedFunction<OomAware, T, OomException> generateSchema) {
			this.oomAware = oomAware;
			this.generateSchema = generateSchema;
			schemas = (T[]) Array.newInstance(tClass, length);
		}

		@Override
		@NotNull
		public String toString() {
			return nestedToString();
		}

		@Override
		public void toString(@NotNull Nester nester) {
			nester.append("schemas", schemas);
		}

		@Override
		public @Nullable NbtCompoundSchema schemaForCompound(int index) throws OomException {
			T schema = oomAware.tryRun(o -> generateSchema.accept(o, index));
			schemas[index] = schema;
			return schema;
		}
	}
}
