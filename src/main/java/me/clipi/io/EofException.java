package me.clipi.io;

/**
 * Similar to {@link java.io.EOFException}, but it does not extend {@link java.io.IOException}
 * so that it has to be explicitly caught.
 */
public final class EofException extends Exception {
	private static final long serialVersionUID = -541017794358015573L;
}
