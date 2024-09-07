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

import me.clipi.io.util.function.CheckedConsumer;
import me.clipi.io.util.function.CheckedFunction;
import me.clipi.io.util.function.CheckedRunnable;
import me.clipi.io.util.function.CheckedSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

		default void tryConsume(@NotNull CheckedConsumer<@NotNull OomAware, OomException> memoryExpensiveComputation) throws OomException {
			try {
				memoryExpensiveComputation.accept(this);
				return;
			} catch (OomException | OutOfMemoryError ignored) {
			}
			trySaveFromOom();
			try {
				memoryExpensiveComputation.accept(this);
			} catch (OutOfMemoryError ex) {
				throw OomException.INSTANCE;
			}
		}

		default <R> R tryRun(@NotNull CheckedFunction<@NotNull OomAware, R, OomException> memoryExpensiveComputation) throws OomException {
			try {
				return memoryExpensiveComputation.apply(this);
			} catch (OomException | OutOfMemoryError ignored) {
			}
			trySaveFromOom();
			try {
				return memoryExpensiveComputation.apply(this);
			} catch (OutOfMemoryError ex) {
				throw OomException.INSTANCE;
			}
		}

		@Nullable
		default <R> R tryRunOrNull(@NotNull CheckedFunction<@NotNull OomAware, @NotNull R, OomException> memoryExpensiveComputation) {
			try {
				return memoryExpensiveComputation.apply(this);
			} catch (OomException | OutOfMemoryError ignored) {
			}
			trySaveFromOom();
			try {
				return memoryExpensiveComputation.apply(this);
			} catch (OomException | OutOfMemoryError ex) {
				return null;
			}
		}

		default void tryConsume(@NotNull CheckedRunnable<OomException> memoryExpensiveComputation) throws OomException {
			try {
				memoryExpensiveComputation.run();
				return;
			} catch (OomException | OutOfMemoryError ignored) {
			}
			trySaveFromOom();
			try {
				memoryExpensiveComputation.run();
			} catch (OutOfMemoryError ex) {
				throw OomException.INSTANCE;
			}
		}

		default <R> R tryRun(@NotNull CheckedSupplier<R, OomException> memoryExpensiveComputation) throws OomException {
			try {
				return memoryExpensiveComputation.get();
			} catch (OomException | OutOfMemoryError ignored) {
			}
			trySaveFromOom();
			try {
				return memoryExpensiveComputation.get();
			} catch (OutOfMemoryError ex) {
				throw OomException.INSTANCE;
			}
		}

		@Nullable
		default <R> R tryRunOrNull(@NotNull CheckedSupplier<@NotNull R, OomException> memoryExpensiveComputation) {
			try {
				return memoryExpensiveComputation.get();
			} catch (OomException | OutOfMemoryError ignored) {
			}
			trySaveFromOom();
			try {
				return memoryExpensiveComputation.get();
			} catch (OomException | OutOfMemoryError ex) {
				return null;
			}
		}

		static void tryConsume(
			@Nullable OomAware oomAware, @NotNull CheckedRunnable<OomException> memoryExpensiveComputation) throws OomException {
			if (oomAware != null) {
				oomAware.tryConsume(memoryExpensiveComputation);
			} else {
				try {
					memoryExpensiveComputation.run();
				} catch (OutOfMemoryError ex) {
					throw OomException.INSTANCE;
				}
			}
		}

		static <R> R tryRun(
			@Nullable OomAware oomAware, @NotNull CheckedSupplier<R, OomException> memoryExpensiveComputation) throws OomException {
			if (oomAware != null) {
				return oomAware.tryRun(memoryExpensiveComputation);
			} else {
				try {
					return memoryExpensiveComputation.get();
				} catch (OutOfMemoryError ex) {
					throw OomException.INSTANCE;
				}
			}
		}

		@Nullable
		static <R> R tryRunOrNull(
			@Nullable OomAware oomAware,
			@NotNull CheckedSupplier<@NotNull R, OomException> memoryExpensiveComputation) {
			if (oomAware != null) {
				return oomAware.tryRunOrNull(memoryExpensiveComputation);
			} else {
				try {
					return memoryExpensiveComputation.get();
				} catch (OomException | OutOfMemoryError ex) {
					return null;
				}
			}
		}
	}
}
