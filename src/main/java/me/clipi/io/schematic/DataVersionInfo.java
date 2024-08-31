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

import me.clipi.io.generic_mc.parse.NbtBlockEntity;
import me.clipi.io.generic_mc.parse.NbtEntity;
import me.clipi.io.util.function.PositionObjFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

public class DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType> {

	public final @NotNull Function<@NotNull ResourceType, @Nullable BlockStateBuilder<BlockStateType>> tryParseBlockState;
	public final @NotNull PositionObjFunction<@NotNull BlockStateType, @Nullable BlockType> tryDefaultBlock;
	public final @NotNull PositionObjFunction<
		@NotNull NbtBlockEntity<ResourceType, BlockStateType>,
		@Nullable BlockType
		> tryNbtBlock;
	public final @NotNull Function<@NotNull ResourceType, @Nullable BiomeType> tryParseBiome;
	public final @NotNull Function<@NotNull NbtEntity<ResourceType>, @Nullable EntityType> tryParseEntity;

	public DataVersionInfo(
		@NotNull Function<@NotNull ResourceType, @Nullable BlockStateBuilder<BlockStateType>> tryParseBlockState,
		@NotNull PositionObjFunction<@NotNull BlockStateType, @Nullable BlockType> tryDefaultBlock,
		@NotNull PositionObjFunction<@NotNull NbtBlockEntity<ResourceType, BlockStateType>, @Nullable BlockType> tryNbtBlock,
		@NotNull Function<@NotNull ResourceType, @Nullable BiomeType> tryParseBiome,
		@NotNull Function<@NotNull NbtEntity<ResourceType>, @Nullable EntityType> tryParseEntity) {
		this.tryParseBlockState = Objects.requireNonNull(tryParseBlockState);
		this.tryDefaultBlock = Objects.requireNonNull(tryDefaultBlock);
		this.tryNbtBlock = Objects.requireNonNull(tryNbtBlock);
		this.tryParseBiome = Objects.requireNonNull(tryParseBiome);
		this.tryParseEntity = Objects.requireNonNull(tryParseEntity);
	}

	public static abstract class BlockStateBuilder<BlockStateType> {
		/**
		 * @return whether the given key and value are accepted as valid input
		 */
		public abstract boolean addProperty(@NotNull String key, @NotNull String value);

		@Nullable
		public abstract BlockStateType build();
	}
}
