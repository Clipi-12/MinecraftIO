package me.clipi.io.nbt.exceptions;

public class NbtNegArraySizeException extends Exception {
	private static final long serialVersionUID = -3802911029815959531L;
	public final int attemptedSize;

	public NbtNegArraySizeException(int attemptedSize) {
		super("Attempted array size: " + attemptedSize);
		this.attemptedSize = attemptedSize;
	}
}
