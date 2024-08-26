package me.clipi.io.nbt;

import me.clipi.io.util.NestedToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

/**
 * Represents a NBT List
 *
 * <p>Empty lists will always be represented by the componentType being the End tag and the backing array being null.
 * The backing array will never be null if the list is not empty.
 */
public class NbtList implements NestedToString {
	public final @NotNull NbtType componentType;
	public final @Nullable Object array;

	@Override
	public String toString() {
		return nestedToString();
	}

	@Override
	public void toString(@NotNull Nester nester) {
		nester.append("component type", componentType)
			  .append("size", array == null ? 0 : Array.getLength(array))
			  .append("array", array);
	}

	public static final NbtList EMPTY_LIST = new NbtList();

	private NbtList() {
		// Empty list instance
		this.componentType = NbtType.End;
		this.array = null;
	}

	private NbtList(@NotNull NbtType componentType, @NotNull Object array) {
		this.componentType = componentType;
		this.array = array;
	}

	public static NbtList create(byte @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Byte, array);
	}

	public static NbtList create(short @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Short, array);
	}

	public static NbtList create(int @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Int, array);
	}

	public static NbtList create(long @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Long, array);
	}

	public static NbtList create(float @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Float, array);
	}

	public static NbtList create(double @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Double, array);
	}

	public static NbtList create(byte @NotNull [] @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.ByteArray, array);
	}

	public static NbtList create(int @NotNull [] @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.IntArray, array);
	}

	public static NbtList create(long @NotNull [] @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.LongArray, array);
	}

	public static NbtList create(@NotNull String @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.String, array);
	}

	public static NbtList create(@NotNull NbtList @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.List, array);
	}

	public static NbtList create(@NotNull NbtCompound @NotNull [] array) {
		return array.length == 0 ? EMPTY_LIST : new NbtList(NbtType.Compound, array);
	}
}
