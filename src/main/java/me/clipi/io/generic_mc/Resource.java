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

package me.clipi.io.generic_mc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class Resource {
	public final @NotNull String namespace, key;

	private Resource(@NotNull String namespace, @NotNull String key) {
		this.namespace = namespace;
		this.key = key;
	}

	@Override
	public String toString() {
		return namespace + ':' + key;
	}

	private static int checkedLastIndexOfColon(char @NotNull [] resource) {
		for (int i = resource.length - 1; i >= 0; --i) {
			char c = resource[i];
			if (c == ':') return i;
			if ((c < '0' | c > '9') &
				(c < 'a' | c > 'z') &
				c != '_' & c != '-' & c != '.' & c != '/'
			) return 0;
		}
		return -1;
	}

	public static boolean isIllegalNamespace(@NotNull String namespace) {
		char[] chars = namespace.toCharArray();
		return isIllegalNamespace(chars, chars.length);
	}

	private static boolean isIllegalNamespace(char @NotNull [] resource, int i) {
		--i;
		do {
			char c = resource[i];
			if ((c < '0' | c > '9') &
				(c < 'a' | c > 'z') &
				c != '_' & c != '-' & c != '.'
			) return true;
		} while (--i >= 0);
		return false;
	}

	@Nullable
	public static Resource parse(@NotNull String resource) {
		String namespace = checkFullAndGetNamespace(resource);
		return namespace == null ?
			null :
			namespace.isEmpty() ?
				new Resource("minecraft", resource) :
				new Resource(namespace, resource.substring(namespace.length() + 1));
	}

	@Nullable
	public static Resource parse(@NotNull String resource, @NotNull Predicate<String> isDeniedNamespace) {
		String namespace = checkFullAndGetNamespace(resource);
		if (namespace == null) return null;
		if (namespace.isEmpty()) {
			return isDeniedNamespace.test("minecraft") ?
				null :
				new Resource("minecraft", resource);
		} else {
			return isDeniedNamespace.test(namespace) ?
				null :
				new Resource(namespace, resource.substring(namespace.length() + 1));
		}
	}

	@Nullable
	private static String checkFullAndGetNamespace(@NotNull String resource) {
		char[] chars = resource.toCharArray();
		int i = checkedLastIndexOfColon(chars);
		if (i < 0) return "";
		int keySize = chars.length - i;
		if ((i == 0 | keySize == 0) || isIllegalNamespace(chars, i)) return null;
		return new String(chars, 0, i);
	}
}
