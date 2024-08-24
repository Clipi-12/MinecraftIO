package me.clipi.io.util.function;

@FunctionalInterface
public interface CheckedByteConsumer<E extends Throwable> {
	/**
	 * Performs this operation on the given byte.
	 *
	 * @param b the input byte
	 */
	void accept(byte b) throws E;
}
