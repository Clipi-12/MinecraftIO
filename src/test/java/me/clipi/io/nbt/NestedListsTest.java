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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

import static me.clipi.io.nbt.NbtTest.getParser;
import static me.clipi.io.nbt.NbtTest.parseByVerifying;

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
		result.append(",\n");
		appendLCLC(result);
		result.append(",\n");
		appendPrimes(result);
		result.append("\n\t}\n}");
		nestedLists = result.toString();
	}

	private static void appendList4Deep(StringBuilder result) {
		// <editor-fold defaultstate="collapsed" desc="code">
		String t0 = "\t\t\t", t1 = t0 + "\t\t", t2 = t1 + "\t\t", t3 = t2 + "\t\t", t4 = t3 + "\t\t";
		String[] tabs = { t0, t1, t2, t3, t4 };

		IntConsumer appendList = i -> {
			String prefix = tabs[i];
			result.append(
				"""
				%scomponent type: NbtType List,
				%ssize: int 4,
				%sarray: NbtList [
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
						%sarray: NbtCompound [
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
		result.append("\t\t}");
		// </editor-fold>
	}

	private static void appendLCLC(StringBuilder result) {
		// <editor-fold defaultstate="collapsed" desc="code">
		BiFunction<Integer, Integer, String> innerCompound = (compound, startingIndex) -> {
			StringBuilder str = new StringBuilder();
			for (int i = startingIndex; i == startingIndex | (i & 3) != 0; ++i) {
				str.append(
					// \t to prevent auto-formatting from messing with the string
					"""
					\t					key #%d of compound #%d: NbtList {
												component type: NbtType List,
												size: int 2,
												array: NbtList [
													{
														component type: NbtType Compound,
														size: int 2,
														array: NbtCompound [
															{
																k1: int %d,
																k2: int %d
															},
															{
																k3: int %d,
																k4: int %d
															}
														]
													},
													{
														component type: NbtType Int,
														size: int 2,
														array: int [%d, %d]
													}
												]
											}""".formatted(i, compound,
														   i << 1, i << 1 | 1,
														   31 - (i << 1), 30 - (i << 1),
														   i, 15 - i));
				if ((i & 3) != 3)
					str.append(",\n");
			}
			return str.toString();
		};
		result.append(
			"""
					compound of lists of compounds of [[4 keys, 4 keys, 4 keys, 4 keys], [index, 15-index]]: NbtCompound {
						compound #0: NbtList {
							component type: NbtType Compound,
							size: int 4,
							array: NbtCompound [
								{
			%s
								},
								{
			%s
								},
								{
			%s
								},
								{
			%s
								}
							]
						},
						compound #1: NbtList {
							component type: NbtType Compound,
							size: int 4,
							array: NbtCompound [
								{
			%s
								},
								{
			%s
								},
								{
			%s
								},
								{
			%s
								}
							]
						},
						compound #2: NbtList {
							component type: NbtType Compound,
							size: int 4,
							array: NbtCompound [
								{
			%s
								},
								{
			%s
								},
								{
			%s
								},
								{
			%s
								}
							]
						},
						compound #3: NbtList {
							component type: NbtType Compound,
							size: int 4,
							array: NbtCompound [
								{
			%s
								},
								{
			%s
								},
								{
			%s
								},
								{
			%s
								}
							]
						}
					}""".formatted(
				innerCompound.apply(0, 0),
				innerCompound.apply(0, 4),
				innerCompound.apply(0, 8),
				innerCompound.apply(0, 12),
				innerCompound.apply(1, 0),
				innerCompound.apply(1, 4),
				innerCompound.apply(1, 8),
				innerCompound.apply(1, 12),
				innerCompound.apply(2, 0),
				innerCompound.apply(2, 4),
				innerCompound.apply(2, 8),
				innerCompound.apply(2, 12),
				innerCompound.apply(3, 0),
				innerCompound.apply(3, 4),
				innerCompound.apply(3, 8),
				innerCompound.apply(3, 12)
			));
		// </editor-fold>
	}

	private static int naivePrime(int n) {
		if (n == 2) --n;
		assert (n & 1) != 0 | n == 0;
		next:
		for (; ; ) {
			n += 2;
			for (int i = 2; i < n; ++i) {
				if (n % i == 0) continue next;
			}
			return n;
		}
	}

	private static void appendPrimes(StringBuilder result) {
		NbtList[] primes = new NbtList[3];
		for (int prime = 0, i = 0; i < 3; ++i) {
			NbtList[] c = new NbtList[4];
			for (int j = 0; j < 4; ++j) {
				NbtList[] b = new NbtList[5];
				for (int k = 0; k < 5; ++k) {
					int[] a = new int[10];
					for (int l = 0; l < 10; ++l)
						a[l] = prime = naivePrime(prime);
					b[k] = NbtList.create(a);
				}
				c[j] = NbtList.create(b);
			}
			primes[i] = NbtList.create(c);
		}

		result.append("\t\t3x4x5x10 nested list of the first prime numbers: ").append(
			NbtList.create(primes)
				   .nestedToString()
				   .lines()
				   .map(l -> "\t\t" + l)
				   .collect(Collectors.joining("\n"))
				   .substring(2)
		);
	}

	@Test
	public void testNestedLists() throws Throwable {
		try (NbtParser<IOException> parser = getParser("nbt/nested-lists.nbt.gz")) {
			Assertions.assertEquals(nestedLists, parseByVerifying(parser).nestedToString());
		}
	}
}