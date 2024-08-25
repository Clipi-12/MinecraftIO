package me.clipi.io.nbt.exceptions;

public class NbtUnknownTagTypeException extends Exception {
	private static final long serialVersionUID = 3311818996793533351L;

	public final int type;

	public NbtUnknownTagTypeException(int type) {
		super("Unknown NBT tag type " + type);
		this.type = type;
	}
}
