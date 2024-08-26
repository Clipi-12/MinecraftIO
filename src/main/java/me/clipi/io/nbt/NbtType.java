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

public enum NbtType {
	End, Byte, Short, Int, Long, Float, Double, String, List, Compound, ByteArray, IntArray, LongArray;

	public final int id = ordinal();

	public static NbtType getByIdOrNull(int id) {
		return id >= 0 && id <= LongArray.id ? values()[id] : null;
	}

	/**
	 * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-4.html#jls-4.12.4">
	 * JLS 4.12.4 (<em>constant variables</em>)</a>
	 * @see <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-13.html#jls-13.1">
	 * JLS 13.1.3 (<em>constant variable</em> references)</a>
	 */
	static final byte
		tagEnd = 0,
		tagByte = 1,
		tagShort = 2,
		tagInt = 3,
		tagLong = 4,
		tagFloat = 5,
		tagDouble = 6,
		tagByteArray = 7,
		tagString = 8,
		tagList = 9,
		tagCompound = 10,
		tagIntArray = 11,
		tagLongArray = 12;
}
