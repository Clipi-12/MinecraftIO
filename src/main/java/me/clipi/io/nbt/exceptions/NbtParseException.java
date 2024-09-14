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

import me.clipi.io.CheckedBigEndianDataInput.ModifiedUtf8DataFormatException;
import me.clipi.io.OomException;
import me.clipi.io.OomException.OomAware;
import me.clipi.io.nbt.NbtType;
import me.clipi.io.util.NestedToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class NbtParseException extends Exception {
	private static final long serialVersionUID = 8384205821108573134L;

	/**
	 * The message is wrapped in a copy-constructor to allow == comparisons
	 */
	@SuppressWarnings("StringOperationCanBeSimplified")
	static final String oomMsg = new String(
		"An OOM error was thrown while trying to create the exception message");

	@NotNull
	static String msg(@Nullable OomAware oomAware, @NotNull Supplier<@NotNull String> msg) {
		String res = OomAware.tryRunOrNull(oomAware, msg::get);
		return res == null ? oomMsg : res;
	}

	private NbtParseException(@Nullable OomAware oomAware, @NotNull Supplier<@NotNull String> msg) {
		super(msg(oomAware, msg));
		// noinspection StringEquality
		if (getMessage() == oomMsg) addSuppressed(OomException.INSTANCE);
	}

	private NbtParseException(@NotNull Throwable cause) {
		super(Objects.requireNonNull(cause));
	}

	private NbtParseException(@Nullable OomAware oomAware, @NotNull Supplier<@NotNull String> msg,
							  @NotNull Throwable cause) {
		super(msg(oomAware, msg), Objects.requireNonNull(cause));
		// noinspection StringEquality
		if (getMessage() == oomMsg) addSuppressed(OomException.INSTANCE);
	}

	public static class UnexpectedTagType extends NbtParseException {
		private static final long serialVersionUID = 7425729617482087567L;

		public final @Nullable NbtType expectedType;
		public final int actualType;

		public UnexpectedTagType(@Nullable NbtType expectedType, int actualType) {
			super(null, () -> expectedType == null ?
				"Unexpected type " + actualType :
				"Expected type " + expectedType + " but received ID " + actualType);
			this.expectedType = expectedType;
			this.actualType = actualType;
		}
	}

	public static class UnknownTagType extends NbtParseException {
		private static final long serialVersionUID = -2836721112540744467L;

		public final int type;

		public UnknownTagType(int type) {
			super(null, () -> "Unknown NBT tag type " + type);
			this.type = type;
		}
	}

	public static class InvalidDataStructureSize extends NbtParseException {
		private static final long serialVersionUID = -1879877328482606036L;

		public final int attemptedSize;

		public InvalidDataStructureSize(int attemptedSize) {
			super(null, () -> "Attempted to create an NBT data structure of size " + attemptedSize);
			this.attemptedSize = attemptedSize;
		}
	}

	public static class DuplicatedKey extends NbtParseException {
		private static final long serialVersionUID = -482101492020003634L;

		public final transient @NotNull NestedToString compoundBeingConstructed;
		public final @NotNull String key;

		public DuplicatedKey(@Nullable OomAware oomAware, @NotNull String key,
							 @NotNull NestedToString compoundBeingConstructed) {
			super(oomAware, () ->
				"Key " + key + " is already present in the NBT Compound represented by: " + compoundBeingConstructed);
			this.key = key;
			this.compoundBeingConstructed = compoundBeingConstructed;
		}
	}

	public static class InvalidString extends NbtParseException {
		private static final long serialVersionUID = -5635255741545922607L;

		public InvalidString(@NotNull ModifiedUtf8DataFormatException cause) {
			super(cause);
		}
	}

	public static class EofException extends NbtParseException {
		private static final long serialVersionUID = -5635255741545922607L;

		public EofException(@NotNull me.clipi.io.EofException cause) {
			super(null, () -> "The reader reached EOF unexpectedly", cause);
		}
	}

	public static class NotEofException extends NbtParseException {
		private static final long serialVersionUID = -5635255741545922607L;

		public NotEofException(@NotNull me.clipi.io.NotEofException cause) {
			super(null, () -> "The reader was supposed to reach EOF, but it did not", cause);
		}
	}

	public static class IncorrectSchema extends NbtParseException {
		private static final long serialVersionUID = -5635255741545922607L;

		public IncorrectSchema(@Nullable OomAware oomAware, @NotNull Object schema) {
			super(oomAware, () -> "The specified schema was not met: " + schema);
		}

		public IncorrectSchema(@Nullable OomAware oomAware, @NotNull Object schema, @NotNull Throwable cause) {
			super(oomAware, () -> "The specified schema was not met: " + schema, cause);
		}
	}
}
