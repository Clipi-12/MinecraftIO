package me.clipi.io.nbt.exceptions;

public class NbtListOfVoidException extends Exception {
	private static final long serialVersionUID = 1609608515500658519L;

	public NbtListOfVoidException() {
		super("Tried parsing a NBT List with an End component type");
	}
}
