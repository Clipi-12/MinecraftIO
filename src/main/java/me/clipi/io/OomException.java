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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Similar to {@link OutOfMemoryError}, but it does not extend {@link Error}
 * so that it has to be explicitly caught.
 */
public final class OomException extends Exception {
	private static final long serialVersionUID = -5526955873890302452L;
	public static final OomException INSTANCE = new OomException();

	private OomException() {
		super(null, null, false, false);
	}

	public interface OomAware {
		void trySaveFromOom();

		default <R> R tryRun(@NotNull Supplier<R> memoryExpensiveComputation) throws OomException {
			return tryRun(this, memoryExpensiveComputation);
		}

		default <R> R tryRunOrNull(@NotNull Supplier<@NotNull R> memoryExpensiveComputation) {
			return tryRunOrNull(this, memoryExpensiveComputation);
		}

		static <R> R tryRun(@Nullable OomAware oomAware, @NotNull Supplier<R> memoryExpensiveComputation) throws OomException {
			try {
				return memoryExpensiveComputation.get();
			} catch (OutOfMemoryError ignored) {
			}
			if (oomAware != null) {
				oomAware.trySaveFromOom();
				try {
					return memoryExpensiveComputation.get();
				} catch (OutOfMemoryError ignored) {
				}
			}
			throw OomException.INSTANCE;
		}

		@Nullable
		static <R> R tryRunOrNull(
			@Nullable OomAware oomAware, @NotNull Supplier<@NotNull R> memoryExpensiveComputation) {
			try {
				return memoryExpensiveComputation.get();
			} catch (OutOfMemoryError ignored) {
			}
			if (oomAware != null) {
				oomAware.trySaveFromOom();
				try {
					return memoryExpensiveComputation.get();
				} catch (OutOfMemoryError ignored) {
				}
			}
			return null;
		}
	}
}
