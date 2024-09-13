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

import java.util.Objects;

public class SaveCompoundSchema extends AllowAllCompoundSchema {
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

	protected SaveCompoundSchema(@NotNull OomAware oomAware) throws OomException {
		compound = nbtCompoundConstructor.apply(Objects.requireNonNull(oomAware));
		this.oomAware = oomAware;
	}

	@Override
	public void toString(@NotNull Nester nester) {
		compound.toString(nester);
	}

	@Override
	@Nullable
	public NbtListOfListsSchema schemaForListOfLists(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return oomAware.tryRun(() -> new ListOfObjects(oomAware));
	}

	@Override
	@Nullable
	public NbtListOfCompoundsSchema schemaForListOfCompounds(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return NbtListOfCompoundsSchema.ListOfSchemas.save(oomAware, length);
	}

	@Override
	@Nullable
	public NbtCompoundSchema schemaForCompound(@NotNull String key) throws OomException {
		return SaveCompoundSchema.create(oomAware);
	}

	private static final class ListOfObjects extends AllowAllListOfListsSchema {
		private final @NotNull OomAware oomAware;

		private ListOfObjects(@NotNull OomAware oomAware) {
			this.oomAware = oomAware;
		}

		@NotNull
		public NbtListOfListsSchema schemaForListOfLists(
			int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
			return this;
		}

		@NotNull
		public NbtListOfCompoundsSchema schemaForListOfCompounds(
			int index, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
			return NbtListOfCompoundsSchema.ListOfSchemas.save(oomAware, length);
		}
	}
}
