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

import me.clipi.io.util.NestedToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Block implements NestedToString {
	public final @NotNull BlockState blockState;
	public final @Nullable BlockEntity blockEntity;
	public final int x, y, z;

	public Block(@NotNull BlockState blockState, @Nullable BlockEntity blockEntity, int x, int y, int z) {
		if (blockEntity != null && (blockEntity.x != x | blockEntity.y != y | blockEntity.z != z))
			throw new IllegalArgumentException();
		this.blockState = Objects.requireNonNull(blockState);
		this.blockEntity = blockEntity;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	@NotNull
	public String toString() {
		return nestedToString();
	}

	@Override
	public void toString(@NotNull Nester nester) {
		nester.append("position", new int[] { x, y, z })
			  .append("block state", blockState)
			  .append("block entity", blockEntity);
	}
}
