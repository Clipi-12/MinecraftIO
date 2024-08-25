package me.clipi.io.nbt.exceptions;

import me.clipi.io.nbt.NbtType;
import org.jetbrains.annotations.NotNull;


public class NbtTypeParseException extends Exception {
	private static final long serialVersionUID = -7278257937126350688L;
	public final NbtType expectedType;
	public final int actualType;

	public NbtTypeParseException(@NotNull NbtType expectedType, int actualType) {
		super("Expected type " + expectedType + " but received ID " + actualType);
		this.expectedType = expectedType;
		this.actualType = actualType;
	}
}
