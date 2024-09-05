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
import me.clipi.io.generic_mc.*;
import me.clipi.io.nbt.NbtParser;
import me.clipi.io.nbt.exceptions.NbtParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.function.IntFunction;

import static me.clipi.io.TestUtils.getString;
import static me.clipi.io.nbt.NbtTest.getParser;

public class SchematicTest {
	private static final @NotNull IntFunction<@Nullable DataVersionInfo<Resource, BlockState, Block, Biome, Entity>> tryDataVersionInfo =
		dataVersion -> dataVersion != 3953 ? null : new DataVersionInfo<>(
			Resource::parse,
			id -> new DataVersionInfo.BlockStateBuilder<>() {
				private final LinkedHashMap<String, String> state = new LinkedHashMap<>();

				@Override
				public boolean addProperty(@NotNull String key, @NotNull String value) {
					state.put(key, value);
					return true;
				}

				@Override
				@NotNull
				public BlockState build() {
					return new BlockState(id, state);
				}
			},
			(x, y, z, blockState) -> new Block(blockState, null, x, y, z),
			(nbtBlockState) -> new Block(
				nbtBlockState.blockState,
				new BlockEntity(nbtBlockState.data, nbtBlockState.x, nbtBlockState.y, nbtBlockState.z),
				nbtBlockState.x, nbtBlockState.y, nbtBlockState.z
			),
			Biome::new,
			entity -> new Entity(entity.id, entity.data, entity.x, entity.y, entity.z)
		);

	@Test
	public void testNatural() throws IOException, OomException, NbtParseException {
		String expected = getString("schematic/output-natural.txt");
		try (NbtParser<IOException> parser = getParser("schematic/natural.schem")) {
			Assertions.assertEquals(
				expected,
				Schematic.parse(
					parser,
					BlockState.class, Block.class, Biome.class, Entity.class, Resource::parse, tryDataVersionInfo
				).nestedToString()
			);
		}
		try (NbtParser<IOException> parser = getParser("schematic/natural.schem")) {
			Assertions.assertEquals(
				expected,
				Schematic.parse(
					parser.parseRoot(),
					BlockState.class, Block.class, Biome.class, Entity.class, Resource::parse, tryDataVersionInfo
				).nestedToString()
			);
		}
	}
}
