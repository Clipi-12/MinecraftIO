package me.clipi.io.nbt;

public class NbtArraySizeParseException extends Exception {
	private static final long serialVersionUID = -3802911029815959531L;
	public final int attemptedSize;

	public NbtArraySizeParseException(int attemptedSize) {
		super("Attempted array size: " + attemptedSize);
		this.attemptedSize = attemptedSize;
	}
}
