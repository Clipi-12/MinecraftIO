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

import me.clipi.io.OomException;
import me.clipi.io.nbt.NbtParser;
import me.clipi.io.nbt.NbtRoot;
import me.clipi.io.nbt.NbtVerifier;
import me.clipi.io.nbt.exceptions.NbtParseException;
import me.clipi.io.util.NestedToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;

public class Schematic<BlockType, BiomeType, EntityType> implements NestedToString {
	public final int dataVersion;
	public final int xOff, yOff, zOff;

	public final @Range(from = 0, to = (1 << 16) - 1) int xLen, yLen, zLen;
	public final @NotNull BlockType @Nullable [] @NotNull [] @NotNull [] yzxBlocks;
	public final @NotNull BiomeType @Nullable [] @NotNull [] @NotNull [] yzxBiomes;
	public final @NotNull EntityType @Nullable [] entities;

	public Schematic(int dataVersion, int xOff, int yOff, int zOff,
					 @Range(from = 0, to = (1 << 16) - 1) int xLen,
					 @Range(from = 0, to = (1 << 16) - 1) int yLen,
					 @Range(from = 0, to = (1 << 16) - 1) int zLen,
					 @NotNull BlockType @Nullable [] @NotNull [] @NotNull [] yzxBlocks,
					 @NotNull BiomeType @Nullable [] @NotNull [] @NotNull [] yzxBiomes,
					 @NotNull EntityType @Nullable [] entities) {
		this.dataVersion = dataVersion;
		this.xOff = xOff;
		this.yOff = yOff;
		this.zOff = zOff;
		this.xLen = xLen;
		this.yLen = yLen;
		this.zLen = zLen;
		this.yzxBlocks = yzxBlocks;
		this.yzxBiomes = yzxBiomes;
		this.entities = entities;
	}

	@NotNull
	public static <ReadException extends Exception, ResourceType, BlockStateType, BlockType, BiomeType, EntityType>
	Schematic<BlockType, BiomeType, EntityType> parse(
		@NotNull NbtParser<ReadException> parser,
		@NotNull Class<BlockStateType> blockStateClass,
		@NotNull Class<BlockType> blockClass,
		@NotNull Class<BiomeType> biomeClass,
		@NotNull Class<EntityType> entityClass,
		@NotNull Function<@NotNull String, @Nullable ResourceType> tryParseResource,
		@NotNull IntFunction<@Nullable DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType>> tryDataVersionInfo)
		throws ReadException, OomException, NbtParseException {
		return parser.parseRoot(new SpongeV3Root<>(
			Objects.requireNonNull(blockStateClass), Objects.requireNonNull(blockClass),
			Objects.requireNonNull(biomeClass), Objects.requireNonNull(entityClass),
			Objects.requireNonNull(tryParseResource), Objects.requireNonNull(tryDataVersionInfo)
		)).schema.schematic;
	}

	@NotNull
	public static <ResourceType, BlockStateType, BlockType, BiomeType, EntityType>
	Schematic<BlockType, BiomeType, EntityType> parse(
		@NotNull NbtRoot nbt,
		@NotNull Class<BlockStateType> blockStateClass,
		@NotNull Class<BlockType> blockClass,
		@NotNull Class<BiomeType> biomeClass,
		@NotNull Class<EntityType> entityClass,
		@NotNull Function<@NotNull String, @Nullable ResourceType> tryParseResource,
		@NotNull IntFunction<@Nullable DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType>> tryDataVersionInfo)
		throws OomException, NbtParseException.IncorrectSchema {
		try {
			return NbtVerifier.verifyRoot(nbt.rootValue, nbt, new SpongeV3Root<>(
				Objects.requireNonNull(blockStateClass), Objects.requireNonNull(blockClass),
				Objects.requireNonNull(biomeClass), Objects.requireNonNull(entityClass),
				Objects.requireNonNull(tryParseResource), Objects.requireNonNull(tryDataVersionInfo)
			)).schema.schematic;
		} catch (NbtParseException.DuplicatedKey ex) {
			// The current schema tree doesn't reuse schemas
			throw new AssertionError(ex);
		}
	}

	@Override
	@NotNull
	public String toString() {
		return nestedToString();
	}

	@Override
	public void toString(@NotNull Nester nester) {
		nester.append("data version", dataVersion)
			  .append("offset", new int[] { xOff, yOff, zOff })
			  .append("dimensions", new int[] { xLen, yLen, zLen })
			  .append("blocks in y,z,x order", yzxBlocks)
			  .append("biomes in y,z,x order", yzxBiomes)
			  .append("entities", entities);
	}
}
