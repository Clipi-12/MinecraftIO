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

import me.clipi.io.CheckedBigEndianDataInput;
import me.clipi.io.CheckedReader;
import me.clipi.io.OomException;
import me.clipi.io.TestUtils;
import me.clipi.io.nbt.exceptions.NbtParseException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static me.clipi.io.TestUtils.getString;

/**
 * NBT test files located in <pre>nbt/all-types.nbt.gz</pre> and <pre>nbt/bigtest.nbt.gz</pre> are
 * <a href="https://github.com/EngineHub/lin-bus/tree/2ecff1b22e7eaca6b7410cc13f1c0d7ddd39e3c3/shared-test-resources">
 * provided by EngineHub</a>
 */
public class NbtTest {
	@NotNull
	@SuppressWarnings("unchecked")
	public static NbtParser<IOException> getParser(@NotNull String @NotNull ... resources) {
		return new NbtParser<>(new CheckedBigEndianDataInput<>(CheckedReader.concat(
			Arrays.stream(resources)
				  .map(TestUtils::resource)
				  .map(TestUtils::gunzip)
				  .map(CheckedReader::fromIs)
				  .toArray(CheckedReader[]::new)
		)));
	}

	@NotNull
	static NbtRoot parseByVerifying(@NotNull NbtParser<IOException> root)
		throws IOException, OomException, NbtParseException {
		var res = root.parseRoot((name, oomAware) -> new SaveCompoundSchema(oomAware) {
			private final @NotNull String rootName = name;
		});
		return new NbtRoot(res.rootName, res.compound);
	}

	@Test
	public void testAllTypes() throws IOException, OomException, NbtParseException {
		String expected = getString("nbt/output-all-types.txt");
		try (NbtParser<IOException> parser = getParser("nbt/all-types.nbt.gz")) {
			Assertions.assertEquals(expected, parser.parseRoot().nestedToString());
		}
		try (NbtParser<IOException> parser = getParser("nbt/all-types.nbt.gz")) {
			Assertions.assertEquals(expected, parseByVerifying(parser).nestedToString());
		}
	}

	@Test
	public void testBigTest() throws IOException, OomException, NbtParseException {
		String expected = getString("nbt/output-bigtest.txt");
		try (NbtParser<IOException> parser = getParser("nbt/bigtest.nbt.gz")) {
			Assertions.assertEquals(expected, parser.parseRoot().nestedToString());
		}
		try (NbtParser<IOException> parser = getParser("nbt/bigtest.nbt.gz")) {
			Assertions.assertEquals(expected, parseByVerifying(parser).nestedToString());
		}
	}

	@Test
	public void testMultipleInputs() throws Throwable {
		String expected1 = getString("nbt/output-all-types.txt");
		String expected2 = getString("nbt/output-bigtest.txt");
		try (NbtParser<IOException> parser = getParser("nbt/all-types.nbt.gz", "nbt/bigtest.nbt.gz")) {
			Assertions.assertEquals(expected1, parser.parseRoot().nestedToString());
			Assertions.assertEquals(expected2, parser.parseRoot().nestedToString());
		}
		try (NbtParser<IOException> parser = getParser("nbt/all-types.nbt.gz", "nbt/bigtest.nbt.gz")) {
			Assertions.assertEquals(expected1, parseByVerifying(parser).nestedToString());
			Assertions.assertEquals(expected2, parseByVerifying(parser).nestedToString());
		}
	}
}