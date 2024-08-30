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
import me.clipi.io.nbt.exceptions.NbtParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

import static me.clipi.io.nbt.NbtTest.getParser;

public class NestedListsTest {
	private static final String nestedLists;

	static {
		StringBuilder result = new StringBuilder(
			"""
			NbtRoot {
				name: "root name",
				root value: NbtCompound {
			""");

		appendList4Deep(result);
		result.append("\t}\n}");
		nestedLists = result.toString();
	}

	private static void appendList4Deep(StringBuilder result) {
		String t0 = "\t\t\t", t1 = t0 + "\t\t", t2 = t1 + "\t\t", t3 = t2 + "\t\t", t4 = t3 + "\t\t";
		String[] tabs = { t0, t1, t2, t3, t4 };

		IntConsumer appendList = i -> {
			String prefix = tabs[i];
			result.append(
				"""
				%scomponent type: NbtType List,
				%ssize: int 4,
				%sarray: NbtList[] [
				%s	{
				""".formatted(prefix, prefix, prefix, prefix));
		};

		BiConsumer<Integer, Boolean> endIterOfList = (i, isLast) -> {
			String prefix = tabs[i];
			result.append(prefix).append("\t}");
			if (isLast) {
				result.append("\n").append(prefix).append("]\n");
			} else {
				result.append(",\n").append(prefix).append("\t{\n");
			}
		};

		result.append("\t\tlists nested 4 levels deep: NbtList {\n");
		appendList.accept(0);
		for (int i = 0; i < 4; ++i) {
			appendList.accept(1);
			for (int j = 0; j < 4; ++j) {
				appendList.accept(2);
				int count = 0;
				for (int k = 0; k < 4; ++k) {
					result.append(
						"""
						%scomponent type: NbtType Compound,
						%ssize: int 4,
						%sarray: NbtCompound[] [
						%s	{
						""".formatted(t3, t3, t3, t3));
					for (int l = 0; l < 4; ++l) {
						result.append(
							"""
							%skey ① of compound [%d,%d] #%d: "value %d",
							%skey ❷ of compound [%d,%d] #%d: int %d
							""".formatted(
								t4, i, j, count, count,
								t4, i, j, count++, 16 - count));
						endIterOfList.accept(3, l == 3);
					}
					endIterOfList.accept(2, k == 3);
				}
				endIterOfList.accept(1, j == 3);
			}
			endIterOfList.accept(0, i == 3);
		}
		result.append("\t\t}\n");
	}

	@Test
	public void testNestedLists() throws IOException, OomException, NbtParseException {
		try (NbtParser<IOException> parser = getParser("nbt/nested-lists.nbt.gz")) {
			Assertions.assertEquals(nestedLists, parser.parseRoot().nestedToString());
		}
	}
}