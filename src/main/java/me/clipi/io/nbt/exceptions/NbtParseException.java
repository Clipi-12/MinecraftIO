package me.clipi.io.nbt.exceptions;

import me.clipi.io.CheckedBigEndianDataInput;
import me.clipi.io.nbt.NbtCompound;
import me.clipi.io.nbt.NbtType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class NbtParseException extends Exception {
	private static final long serialVersionUID = 8384205821108573134L;

	private NbtParseException(@NotNull String msg) {
		super(msg);
	}

	private NbtParseException(@NotNull Throwable cause) {
		super(cause);
	}

	public static class UnexpectedTagType extends NbtParseException {
		private static final long serialVersionUID = 7425729617482087567L;

		public final @Nullable NbtType expectedType;
		public final int actualType;

		public UnexpectedTagType(@Nullable NbtType expectedType, int actualType) {
			super(expectedType == null ?
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
			super("Unknown NBT tag type " + type);
			this.type = type;
		}
	}

	public static class InvalidArraySize extends NbtParseException {
		private static final long serialVersionUID = 1744567714372945502L;

		public final int attemptedSize;

		public InvalidArraySize(int attemptedSize) {
			super("Invalid array size: " + attemptedSize);
			this.attemptedSize = attemptedSize;
		}
	}

	public static class DuplicatedKey extends NbtParseException {
		private static final long serialVersionUID = -482101492020003634L;

		public final @NotNull NbtCompound compoundBeingConstructed;
		public final @NotNull String key;

		public DuplicatedKey(@NotNull String key, @NotNull NbtCompound compoundBeingConstructed) {
			super("Key " + key + " is already present in the NBT Compound " + compoundBeingConstructed);
			this.key = key;
			this.compoundBeingConstructed = compoundBeingConstructed;
		}
	}

	public static class InvalidString extends NbtParseException {
		private static final long serialVersionUID = -5635255741545922607L;

		public InvalidString(@NotNull CheckedBigEndianDataInput.ModifiedUtf8DataFormatException cause) {
			super(cause);
		}
	}
}
