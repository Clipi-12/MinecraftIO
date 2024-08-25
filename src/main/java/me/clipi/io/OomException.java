package me.clipi.io;

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
}
