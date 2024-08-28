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

package me.clipi.io.nbt.exceptions;

import me.clipi.io.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

public class NbtKeyNotFoundException extends Exception {
	private static final long serialVersionUID = -3525208150628883798L;

	public final @NotNull NbtCompound compoundBeingConstructed;
	public final @NotNull String key;

	public NbtKeyNotFoundException(@NotNull String key, @NotNull NbtCompound compound) {
		super("Key " + key + " is not present in the NBT Compound " + compound.nestedToString());
		this.key = key;
		this.compoundBeingConstructed = compound;
	}
}
