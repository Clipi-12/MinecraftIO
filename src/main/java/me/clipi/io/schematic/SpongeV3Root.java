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

package me.clipi.io.schematic;

import me.clipi.io.nbt.schema.DenyAllCompoundSchema;
import me.clipi.io.nbt.schema.NbtCompoundSchema;
import me.clipi.io.nbt.schema.NbtRootSchema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.IntFunction;

import static me.clipi.io.OomException.OomAware;

/**
 * package-private
 */
class SpongeV3Root<ResourceType, BlockStateType, BlockType, BiomeType, EntityType>
	implements NbtRootSchema<SpongeV3Root<ResourceType, BlockStateType, BlockType, BiomeType, EntityType>.SpongeV3Holder> {
	private final Class<BlockStateType> blockStateClass;
	private final Class<BlockType> blockClass;
	private final Class<BiomeType> biomeClass;
	private final Class<EntityType> entityClass;
	private final Function<@NotNull String, @Nullable ResourceType> tryParseResource;
	private final IntFunction<
		@Nullable DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType>> tryDataVersionInfo;

	SpongeV3Root(@NotNull Class<BlockStateType> blockStateClass,
				 @NotNull Class<BlockType> blockClass,
				 @NotNull Class<BiomeType> biomeClass,
				 @NotNull Class<EntityType> entityClass,
				 @NotNull Function<@NotNull String, @Nullable ResourceType> tryParseResource,
				 @NotNull IntFunction<
					 @Nullable DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType>> tryDataVersionInfo) {
		this.blockStateClass = blockStateClass;
		this.blockClass = blockClass;
		this.biomeClass = biomeClass;
		this.entityClass = entityClass;
		this.tryParseResource = tryParseResource;
		this.tryDataVersionInfo = tryDataVersionInfo;
	}

	@Override
	public @Nullable SpongeV3Holder schemaForRootValue(@NotNull String rootName, @NotNull OomAware oomAware) {
		return rootName.isEmpty() ? new SpongeV3Holder(oomAware) : null;
	}

	final class SpongeV3Holder extends DenyAllCompoundSchema {
		private final OomAware oomAware;
		/**
		 * package-private
		 */
		SpongeV3Schema<ResourceType, BlockStateType, BlockType, BiomeType, EntityType> schema;

		SpongeV3Holder(@NotNull OomAware oomAware) {
			this.oomAware = oomAware;
		}

		@Override
		public boolean deniesFinishedCompound() {
			return false;
		}

		@Override
		public @Nullable NbtCompoundSchema schemaForCompound(@NotNull String key) {
			return "Schematic".equals(key) ?
				schema = new SpongeV3Schema<>(oomAware, blockStateClass, blockClass, biomeClass, entityClass,
											  tryParseResource, tryDataVersionInfo) :
				null;
		}

		@Override
		public void toString(@NotNull Nester nester) {
			nester.append("schema", schema);
		}
	}
}
