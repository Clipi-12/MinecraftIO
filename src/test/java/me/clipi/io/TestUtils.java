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

package me.clipi.io;

import me.clipi.io.nbt.NbtTest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

public class TestUtils {
	private static final ClassLoader cl = NbtTest.class.getClassLoader();

	@NotNull
	public static InputStream resource(@NotNull String resource) {
		return Objects.requireNonNull(cl.getResourceAsStream(resource));
	}

	public static String getString(@NotNull String resource) throws IOException {
		try (InputStream is = resource(resource)) {
			return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim().replace("\r\n", "\n");
		}
	}

	@NotNull
	public static GZIPInputStream gunzip(@NotNull InputStream is) {
		try {
			return new GZIPInputStream(is);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
