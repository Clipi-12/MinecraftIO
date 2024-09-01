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
import me.clipi.io.nbt.NbtVerifier;
import me.clipi.io.nbt.SaveCompoundSchema;
import me.clipi.io.util.NestedToString;
import me.clipi.io.util.function.CheckedSupplier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class DelegatedCompoundSchema<T extends NbtCompoundSchema> implements NestedToString {
	private DelegatedCompoundSchema() {
	}

	public static <T extends NbtCompoundSchema> DelegatedCompoundSchema<T> create(
		@NotNull OomAware oomAware, @NotNull CheckedSupplier<@Nullable T, OomException> tryCompute) throws OomException {
		T schema = tryCompute.get();
		return schema == null ?
			oomAware.tryRun(() -> new LazyEvaluation<>(oomAware, tryCompute)) :
			oomAware.tryRun(() -> new EagerEvaluation<>(schema));
	}

	@Override
	@NotNull
	public final String toString() {
		return nestedToString();
	}

	@Nullable
	@Contract(pure = true)
	public abstract T computeSchema() throws OomException;

	@NotNull
	@Contract(pure = true)
	public abstract NbtCompoundSchema rawSchema();

	public static final class EagerEvaluation<T extends NbtCompoundSchema> extends DelegatedCompoundSchema<T> {
		private final @NotNull T schema;

		public EagerEvaluation(@NotNull T schema) {
			this.schema = Objects.requireNonNull(schema);
		}

		@Override
		public void toString(@NotNull Nester nester) {
			nester.append("schema", schema);
		}

		@Override
		@NotNull
		public T computeSchema() {
			return schema;
		}

		@Override
		@NotNull
		public NbtCompoundSchema rawSchema() {
			return schema;
		}
	}

	public static final class LazyEvaluation<T extends NbtCompoundSchema> extends DelegatedCompoundSchema<T> {
		private final @NotNull SaveCompoundSchema rawSchema;
		private final OomAware oomAware;
		private @Nullable CheckedSupplier<@Nullable T, OomException> compute;
		private @Nullable T schema;

		public LazyEvaluation(
			@NotNull OomAware oomAware, @NotNull CheckedSupplier<@Nullable T, OomException> compute) throws OomException {
			this.oomAware = oomAware;
			this.rawSchema = SaveCompoundSchema.create(oomAware);
			this.compute = Objects.requireNonNull(compute);
		}

		@Override
		public void toString(@NotNull Nester nester) {
			nester.append("raw schema", rawSchema);
			if (compute == null) nester.append("schema", schema);
		}

		@Override
		@Nullable
		public T computeSchema() throws OomException {
			if (compute == null) return schema;
			CheckedSupplier<@Nullable T, OomException> compute = this.compute;
			this.compute = null;
			T res = schema = compute.get();
			return res == null || NbtVerifier.isDeniedBySchema(oomAware, rawSchema.compound, res) ? null : res;
		}

		@Override
		@NotNull
		public NbtCompoundSchema rawSchema() {
			return rawSchema;
		}
	}
}
