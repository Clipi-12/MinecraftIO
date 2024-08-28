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
import me.clipi.io.nbt.schema.DenyAllCompoundSchema;
import me.clipi.io.nbt.schema.NbtCompoundSchema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static me.clipi.io.TestUtils.getString;
import static me.clipi.io.nbt.NbtTest.getParser;

public class SchematicTest {
	@NotNull
	public static SpongeV3Schematic<Block, Biome, Entity> read(@NotNull NbtParser<IOException> parser) throws OomException, NbtParseException, IOException {
		return parser.parseRoot((rootName, oomAware) -> {
			if (!rootName.isEmpty()) return null;
			return new DenyAllCompoundSchema() {
				private SpongeV3Schema<Resource, BlockState, Block, Biome, Entity> schema;

				@Override
				public void toString(@NotNull Nester nester) {
				}

				@Override
				public boolean deniesFinishedCompound() {
					return false;
				}

				@Override
				public @Nullable NbtCompoundSchema schemaForCompound(@NotNull String key) {
					if (!"Schematic".equals(key)) return null;
					return schema = new SpongeV3Schema<>(
						oomAware, BlockState.class, Block.class, Biome.class, Entity.class,
						Resource::parse,
						blockState -> {
							int i = blockState.indexOf('[');
							Resource id = Resource.parse(i < 0 ? blockState : blockState.substring(0, i));
							return id == null ? null : new BlockState(id, Map.of("id and state", blockState));
						},
						(x, y, z, blockState) -> new Block(blockState, null, x, y, z),
						(x, y, z, nbtBlockState) -> new Block(
							nbtBlockState.blockState,
							new BlockEntity(nbtBlockState.data, x, y, z),
							x, y, z
						),
						Biome::new,
						entity -> new Entity(entity.id, entity.data, entity.x, entity.y, entity.z)
					);
				}
			};
		}).schema.schematic;
	}

	@Test
	public void testNatural() throws IOException, OomException, NbtParseException {
		try (NbtParser<IOException> parser = getParser("schematic/natural.schem")) {
			Assertions.assertEquals(getString("schematic/output-natural.txt"), read(parser).nestedToString());
		}
	}
}
