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

package me.clipi.io.generic_mc.parse;

import me.clipi.io.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class NbtBlockEntity<ResourceType, BlockStateType> extends NbtResource<ResourceType> {
	public final int x, y, z;
	public final @NotNull BlockStateType blockState;

	public NbtBlockEntity(@NotNull ResourceType id, @NotNull BlockStateType blockState, @Nullable NbtCompound data,
						  int x, int y, int z) {
		super(id, data);
		this.x = x;
		this.y = y;
		this.z = z;
		this.blockState = Objects.requireNonNull(blockState);
	}
}
