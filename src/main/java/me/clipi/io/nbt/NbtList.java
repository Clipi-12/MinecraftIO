package me.clipi.io.nbt;

import org.jetbrains.annotations.NotNull;

/**
 * Empty lists will always be represented by the componentType being the End tag and the backing array being null.
 * The backing array will never be null if the list is not empty.
 */
public class NbtList {
	public final @NotNull NbtType componentType;
	public final Object array;

	public static final NbtList EMPTY_LIST = new NbtList();

	private NbtList() {
		// Empty list instance
		this.componentType = NbtType.End;
		this.array = null;
	}

	public NbtList(byte @NotNull [] array) {
		this.componentType = NbtType.Byte;
		this.array = array;
	}

	public NbtList(short @NotNull [] array) {
		this.componentType = NbtType.Short;
		this.array = array;
	}

	public NbtList(int @NotNull [] array) {
		this.componentType = NbtType.Int;
		this.array = array;
	}

	public NbtList(long @NotNull [] array) {
		this.componentType = NbtType.Long;
		this.array = array;
	}

	public NbtList(float @NotNull [] array) {
		this.componentType = NbtType.Float;
		this.array = array;
	}

	public NbtList(double @NotNull [] array) {
		this.componentType = NbtType.Double;
		this.array = array;
	}

	public NbtList(byte @NotNull [] @NotNull [] array) {
		this.componentType = NbtType.ByteArray;
		this.array = array;
	}

	public NbtList(int @NotNull [] @NotNull [] array) {
		this.componentType = NbtType.IntArray;
		this.array = array;
	}

	public NbtList(long @NotNull [] @NotNull [] array) {
		this.componentType = NbtType.LongArray;
		this.array = array;
	}

	public NbtList(@NotNull String @NotNull [] array) {
		this.componentType = NbtType.String;
		this.array = array;
	}

	public NbtList(@NotNull NbtList @NotNull [] array) {
		this.componentType = NbtType.List;
		this.array = array;
	}

	public NbtList(@NotNull NbtCompound @NotNull [] array) {
		this.componentType = NbtType.Compound;
		this.array = array;
	}
}
