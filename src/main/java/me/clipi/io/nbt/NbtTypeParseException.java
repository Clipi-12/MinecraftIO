package me.clipi.io.nbt;

import org.jetbrains.annotations.NotNull;


public class NbtTypeParseException extends Exception {
	private static final long serialVersionUID = -7278257937126350688L;
	public final NbtParser.Type expectedType;
	public final int actualType;

	public NbtTypeParseException(NbtParser.@NotNull Type expectedType, int actualType) {
		super("Expected type " + expectedType + " but received ID " + actualType);
		this.expectedType = expectedType;
		this.actualType = actualType;
	}
}
