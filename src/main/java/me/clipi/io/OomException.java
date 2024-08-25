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

		default <R> R tryRunOrNull(@NotNull Supplier<R> memoryExpensiveComputation) {
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
		static <R> R tryRunOrNull(@Nullable OomAware oomAware,
								  @NotNull Supplier<@NotNull R> memoryExpensiveComputation) {
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
