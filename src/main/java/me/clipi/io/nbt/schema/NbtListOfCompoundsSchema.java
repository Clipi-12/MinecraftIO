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
import me.clipi.io.nbt.NbtCompound;
import me.clipi.io.nbt.SaveCompoundSchema;
import me.clipi.io.nbt.exceptions.NbtKeyNotFoundException;
import me.clipi.io.nbt.exceptions.NbtParseException;
import me.clipi.io.util.GrowableArray;
import me.clipi.io.util.NestedToString;
import me.clipi.io.util.function.CheckedFunction;
import me.clipi.io.util.function.ObjIntCheckedFunction;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;

@FunctionalInterface
public interface NbtListOfCompoundsSchema {
	@NotNull
	NbtListOfCompoundsSchema ALWAYS = i -> NbtCompoundSchema.ALWAYS;

	default boolean deniesFinishedList() throws OomException, NbtParseException, NbtKeyNotFoundException {
		return false;
	}

	/**
	 * @return The schema for the specified compound, or {@code null} if the compound is not allowed.
	 */
	@Nullable
	NbtCompoundSchema schemaForCompound(int index) throws OomException;

	abstract class ListOfSchemas<T extends NbtCompoundSchema, R> implements NbtListOfCompoundsSchema, NestedToString {
		protected final int length;
		private final R @NotNull [] array;
		protected final @NotNull OomAware oomAware;

		/**
		 * @param length must be the exact length of the expected list
		 */
		public static @NotNull <T extends NbtCompoundSchema> ListOfSchemas<T, T> createWithoutDuplicates(
			@NotNull OomAware oomAware, @NotNull Class<T> tClass,
			@Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length,
			@NotNull CheckedFunction<@NotNull OomAware, @Nullable T, OomException> generateSchema) throws OomException {
			return createWithoutDuplicates(oomAware, tClass, length, (o, i) -> generateSchema.apply(o));
		}

		/**
		 * @param length must be the exact length of the expected list
		 */
		public static @NotNull <T extends NbtCompoundSchema> ListOfSchemas<T, T> createWithoutDuplicates(
			@NotNull OomAware oomAware, @NotNull Class<T> tClass,
			@Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length,
			@NotNull ObjIntCheckedFunction<@NotNull OomAware, @Nullable T, OomException> generateSchema) throws OomException {
			return length == 1 ?
				create(oomAware, tClass, length, generateSchema) :
				oomAware.tryRun(() -> new ListOfDistinctSchemas<T, T>(oomAware, tClass, length) {
					@Override
					protected @Nullable T generateDistinctSchema(@NotNull OomAware oomAware, int index) throws OomException {
						return generateSchema.accept(oomAware, index);
					}

					@Override
					protected @NotNull T mapSchema(@NotNull T schema) {
						return schema;
					}
				});
		}

		/**
		 * @param length must be the exact length of the expected list
		 */
		public static @NotNull <T extends NbtCompoundSchema> ListOfSchemas<T, T> create(
			@NotNull OomAware oomAware, @NotNull Class<T> tClass,
			@Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length,
			@NotNull CheckedFunction<@NotNull OomAware, @Nullable T, OomException> generateSchema) throws OomException {
			return create(oomAware, tClass, length, (o, i) -> generateSchema.apply(o));
		}

		/**
		 * @param length must be the exact length of the expected list
		 */
		public static @NotNull <T extends NbtCompoundSchema> ListOfSchemas<T, T> create(
			@NotNull OomAware oomAware, @NotNull Class<T> tClass,
			@Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length,
			@NotNull ObjIntCheckedFunction<@NotNull OomAware, @Nullable T, OomException> generateSchema) throws OomException {
			return oomAware.tryRun(() -> new ListOfSchemas<T, T>(oomAware, tClass, length) {
				@Override
				protected @Nullable T generateSchema(@NotNull OomAware oomAware, int index) throws OomException {
					return generateSchema.accept(oomAware, index);
				}

				@Override
				protected @NotNull T mapSchema(@NotNull T schema) {
					return schema;
				}
			});
		}

		/**
		 * @param length must be the exact length of the expected list
		 */
		public static @NotNull ListOfSchemas<SaveCompoundSchema, NbtCompound> save(
			@NotNull OomAware oomAware, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
			return oomAware.tryRun(() -> new ListOfSchemas<SaveCompoundSchema, NbtCompound>(
				oomAware, NbtCompound.class, length) {
				@Override
				@NotNull
				protected SaveCompoundSchema generateSchema(@NotNull OomAware oomAware, int index) throws OomException {
					return SaveCompoundSchema.create(oomAware);
				}

				@Override
				protected @NotNull NbtCompound mapSchema(@NotNull SaveCompoundSchema schema) {
					return schema.compound;
				}
			});
		}

		@SuppressWarnings("unchecked")
		public ListOfSchemas(@NotNull OomAware oomAware, @NotNull Class<R> rClass,
							 @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
			// noinspection ConstantValue
			if (length <= 0) throw new IllegalArgumentException();
			this.length = length;
			this.oomAware = oomAware;
			array = (R[]) Array.newInstance(rClass, length);
		}

		@Override
		@NotNull
		public String toString() {
			return nestedToString();
		}

		@Override
		public void toString(@NotNull Nester nester) {
			nester.append("schemas", array);
		}

		protected abstract @Nullable T generateSchema(@NotNull OomAware oomAware, int index) throws OomException;

		protected abstract @NotNull R mapSchema(@NotNull T schema);

		@Override
		public @Nullable T schemaForCompound(int index) throws OomException {
			T schema = oomAware.tryRun(o -> generateSchema(o, index));
			if (schema != null) array[index] = Objects.requireNonNull(mapSchema(schema));
			return schema;
		}

		/**
		 * The mutable array with all the schemas' objects.
		 *
		 * <p>None of the elements will be null once the list has been parsed.
		 */
		public @Nullable R @NotNull [] nullableElements() {
			return array;
		}

		/**
		 * The mutable array with all the schemas' objects.
		 *
		 * <p>Either none of the elements are null, or the array itself is null.
		 */
		public @NotNull R @Nullable [] elementsOrNull() {
			return array[array.length - 1] == null ? null : array;
		}
	}

	abstract class ListOfDistinctSchemas<T extends NbtCompoundSchema, R> extends ListOfSchemas<T, R> {
		// T[] to save the objects by their hash, as a fixed mini-hash-set
		// The extra object is not part of the mini-hash-set, but the value of the last schema, so that the hashcode
		// is only computed once it is finished
		private final Object[] eqs;

		private ListOfDistinctSchemas(@NotNull OomAware oomAware, Class<R> rClass, int length) {
			super(oomAware, rClass, length);
			eqs = new Object[length + 1];
		}

		protected abstract @Nullable T generateDistinctSchema(@NotNull OomAware oomAware, int index) throws OomException;

		@Override
		@MustBeInvokedByOverriders
		public boolean deniesFinishedList() throws OomException, NbtParseException, NbtKeyNotFoundException {
			return denyLast(eqs[length]);
		}

		@Override
		protected final @Nullable T generateSchema(@NotNull OomAware oomAware, int index) throws OomException {
			T res = generateDistinctSchema(oomAware, index);
			if (res == null) return null;
			Object last = eqs[length];
			eqs[length] = res;
			return last != null && denyLast(last) ? null : res;
		}

		private boolean denyLast(@NotNull Object last) {
			final int length = this.length;
			final int hash = last.hashCode();
			final int hashIdx = (hash >> 16) % length;
			Object[] eqs = this.eqs;

			for (int idx = hashIdx; idx >= 0; --idx) {
				Object e = eqs[idx];
				if (e == null) {
					eqs[idx] = last;
					return false;
				}
				if (e.hashCode() == hash && last.equals(e)) return true;
			}
			for (int idx = length - 1; idx > hashIdx; --idx) {
				Object e = eqs[idx];
				if (e == null) {
					eqs[idx] = last;
					return false;
				}
				if (e.hashCode() == hash && last.equals(e)) return true;
			}
			throw new IllegalStateException(String.format(
				"More than %d schemas were provided.%nExtra element: %s%nCurrent elements: %s",
				length, last, Arrays.toString(nullableElements())));
		}
	}
}
