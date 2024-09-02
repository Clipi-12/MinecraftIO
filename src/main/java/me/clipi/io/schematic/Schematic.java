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
	public final int xOffset, yOffset, zOffset;

	public final @Range(from = 0, to = (1 << 16) - 1) int xLen, yLen, zLen;
	/**
	 * The blocks present in this schematic, in a [y,z,x] order
	 * <p>i.e. the element in the coordinate {@code (x,y,z)} is located at the index {@code x + z*xLen + y*xLen*zLen}
	 *
	 * @apiNote The array is either {@code null} or contains {@code xLen*yLen*zLen} entries
	 */
	public final @NotNull BlockType @Nullable [] yzxBlocks;
	/**
	 * The biomes present in this schematic, in a [y,z,x] order
	 * <p>i.e. the element in the coordinate {@code (x,y,z)} is located at the index {@code x + z*xLen + y*xLen*zLen}
	 *
	 * @apiNote The array is either {@code null} or contains {@code xLen*yLen*zLen} entries
	 */
	public final @NotNull BiomeType @Nullable [] yzxBiomes;
	public final @NotNull EntityType @Nullable [] entities;

	/**
	 * @param yzxBlocks either a {@code null} value or an array with {@code xLen*yLen*zLen} blocks in [y,z,x] order
	 * @param yzxBiomes either a {@code null} value or an array with {@code xLen*yLen*zLen} biomes in [y,z,x] order
	 * @throws IllegalArgumentException if the preconditions are not met
	 * @implNote This constructor does not check if each entry of yzxBlocks and yzxBiomes is non-null, despite it
	 * being required if the arrays themselves are not null.
	 * <p>This constructor does not check if the block-entities represented by each block are inside the region
	 * {@code [(0,0,0), (xLen,yLen,zLen)]}, despite it being required.
	 */
	public Schematic(int dataVersion, int xOffset, int yOffset, int zOff,
					 @Range(from = 0, to = (1 << 16) - 1) int xLen,
					 @Range(from = 0, to = (1 << 16) - 1) int yLen,
					 @Range(from = 0, to = (1 << 16) - 1) int zLen,
					 @NotNull BlockType @Nullable [] yzxBlocks,
					 @NotNull BiomeType @Nullable [] yzxBiomes,
					 @NotNull EntityType @Nullable [] entities) {
		preconditions:
		{
			// noinspection ConstantValue
			if (xLen >= 0 & xLen < 1 << 16 &
				yLen >= 0 & yLen < 1 << 16 &
				zLen >= 0 & zLen < 1 << 16) {
				int dim = xLen * yLen * zLen;
				if ((yzxBlocks == null || yzxBlocks.length == dim) &&
					(yzxBiomes == null || yzxBiomes.length == dim)) {
					break preconditions;
				}
			}
			throw new IllegalArgumentException();
		}
		this.dataVersion = dataVersion;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
		this.zOffset = zOff;
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
			  .append("offset", new int[] { xOffset, yOffset, zOffset })
			  .append("dimensions", new int[] { xLen, yLen, zLen })
			  .append("blocks in y,z,x order", yzxBlocks)
			  .append("biomes in y,z,x order", yzxBiomes)
			  .append("entities", entities);
	}
}
