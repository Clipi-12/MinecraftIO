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

package me.clipi.io.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serial;
import java.util.*;

/**
 * Tests from <a href="https://wiki.vg/VarInt_And_VarLong#:~:text=an%20int32.-,Sample,-VarInts%3A">wiki.vg</a>
 */
public class VarIntLongTest {
	private static final Map<byte[], Integer> varInts = new LinkedHashMap<>(11) {
		@Serial
		private static final long serialVersionUID = -385721721595996614L;

		{
			put(new byte[] { (byte) 0x00 }, 0);
			put(new byte[] { (byte) 0x01 }, 1);
			put(new byte[] { (byte) 0x02 }, 2);
			put(new byte[] { (byte) 0x7f }, 127);
			put(new byte[] { (byte) 0x80, (byte) 0x01 }, 128);
			put(new byte[] { (byte) 0xff, (byte) 0x01 }, 255);
			put(new byte[] { (byte) 0xdd, (byte) 0xc7, (byte) 0x01 }, 25565);
			put(new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0x7f }, 2097151);
			put(new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x07 }, 2147483647);
			put(new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x0f }, -1);
			put(new byte[] { (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x08 }, -2147483648);
		}
	};
	private static final Map<byte[], Long> varLongs = new LinkedHashMap<>(11) {
		@Serial
		private static final long serialVersionUID = -385721721595996614L;

		{
			put(new byte[] { (byte) 0x00 }, 0L);
			put(new byte[] { (byte) 0x01 }, 1L);
			put(new byte[] { (byte) 0x02 }, 2L);
			put(new byte[] { (byte) 0x7f }, 127L);
			put(new byte[] { (byte) 0x80, (byte) 0x01 }, 128L);
			put(new byte[] { (byte) 0xff, (byte) 0x01 }, 255L);
			put(new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x07 }, 2147483647L);
			put(new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
							 (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x7f }, 9223372036854775807L);
			put(new byte[] { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
							 (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x01 }, -1L);
			put(new byte[] { (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xf8,
							 (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x01 }, -2147483648L);
			put(new byte[] { (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80,
							 (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x01 }, -9223372036854775808L);
		}
	};

	private static int[] getVarInts(byte[] bytes, int size) {
		int[] res = new int[size];
		Assertions.assertDoesNotThrow(() -> VarIntLong.parseVarInts(bytes, (val, i) -> res[i] = val));
		return res;
	}

	private static long[] getVarLongs(byte[] bytes, int size) {
		long[] res = new long[size];
		Assertions.assertDoesNotThrow(() -> VarIntLong.parseVarLongs(bytes, (val, i) -> res[i] = val));
		return res;
	}

	@Test
	public void testVarInt() {
		varInts.forEach((bytes, expected) -> {
			int result = getVarInts(bytes, 1)[0];
			Assertions.assertEquals(expected, result);
		});
	}

	@Test
	public void testVarLong() {
		varLongs.forEach((bytes, expected) -> {
			long result = getVarLongs(bytes, 1)[0];
			Assertions.assertEquals(expected, result);
		});
	}

	private static byte[] concat(List<byte[]> arrays) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			for (byte[] array : arrays)
				baos.write(array);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return baos.toByteArray();
	}

	@Test
	public void testVarInts() {
		Random rng = new Random(0);
		for (int iter = 100; iter > 0; --iter) {
			ArrayList<Map.Entry<byte[], Integer>> entries = new ArrayList<>(varInts.entrySet());
			Collections.shuffle(entries, rng);
			byte[] input = concat(entries.stream().map(Map.Entry::getKey).toList());
			int[] expected = entries.stream().mapToInt(Map.Entry::getValue).toArray();
			int[] result = getVarInts(input, entries.size());
			int[] asd = getVarInts(input, entries.size());
			Assertions.assertArrayEquals(expected, result);
		}
	}

	@Test
	public void testVarLongs() {
		Random rng = new Random(0);
		for (int iter = 100; iter > 0; --iter) {
			ArrayList<Map.Entry<byte[], Long>> entries = new ArrayList<>(varLongs.entrySet());
			Collections.shuffle(entries, rng);
			byte[] input = concat(entries.stream().map(Map.Entry::getKey).toList());
			long[] expected = entries.stream().mapToLong(Map.Entry::getValue).toArray();
			long[] result = getVarLongs(input, entries.size());
			Assertions.assertArrayEquals(expected, result);
		}
	}
}
