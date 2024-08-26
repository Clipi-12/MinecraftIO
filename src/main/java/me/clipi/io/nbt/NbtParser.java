/*
 * MinecraftIO, a simple library with multiple Minecraft IO-tools
 * Copyright (C) 2024  Clipi (GitHub: Clipi-12)
 *
 * This file is part of MinecraftIO.
 *
 * MinecraftIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MinecraftIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MinecraftIO.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.clipi.io.nbt;

import me.clipi.io.CheckedBigEndianDataInput;
import me.clipi.io.EofException;
import me.clipi.io.NotEofException;
import me.clipi.io.OomException;
import me.clipi.io.OomException.OomAware;
import me.clipi.io.nbt.exceptions.NbtParseException;
import me.clipi.io.util.FixedStack;
import me.clipi.io.util.GrowableArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

/**
 * @see <a href="https://minecraft.wiki/w/NBT_format">NBT format</a>
 */
public class NbtParser<ReadException extends Exception> implements AutoCloseable {
	private final CheckedBigEndianDataInput<ReadException> di;
	/**
	 * FixedStack of objects that are either NbtCompound or ListOfLists.
	 * If the project used Java 17, this could be improved with sealed classes
	 *
	 * <p>The wiki says that <pre>Compound and List tags may not be nested beyond a depth of {@code 512}</pre>,
	 * but we allow for {@code 1024}, and an additional tag for the root
	 */
	private final FixedStack<Object> nestedTarget = new FixedStack<>(Object.class, 1025);
	private @Nullable OomAware oomAware;

	public NbtParser(@NotNull CheckedBigEndianDataInput<ReadException> di) {
		this.di = di;
	}

	@Override
	public void close() throws ReadException {
		di.close();
	}

	public void closeCurrent() throws ReadException {
		di.closeCurrent();
	}

	@NotNull
	public NbtRoot parseRoot() throws ReadException, EofException, NotEofException, OomException,
									  NbtParseException, FixedStack.FullStackException {
		di.expectedByteFail(NbtType.tagCompound, type -> {
			throw new NbtParseException.UnexpectedTagType(NbtType.Compound, type);
		});
		String name = readString();
		NbtCompound root;
		try {
			root = readRootValue();
		} finally {
			di.setOomAware(oomAware = null);
			nestedTarget.clear();
		}
		di.expectEnd();
		closeCurrent();
		return new NbtRoot(name, root);
	}

	@NotNull
	private NbtCompound readRootValue() throws ReadException, EofException, OomException,
											   NbtParseException, FixedStack.FullStackException {
		FixedStack<Object> nestedTarget = this.nestedTarget;
		NbtCompound root = new NbtCompound();
		di.setOomAware(oomAware = root);
		nestedTarget.push(root);
		NbtCompound target = root;
		for (; ; ) {
			ListOfLists nextTarget = readMapEntry(target);
			if (nextTarget == null) return root;
			target = readListEntries(nextTarget);
		}
	}

	/**
	 * @return whether the root has been reached or a ListOfLists target is on top of the stack
	 */
	@Nullable
	private ListOfLists readMapEntry(@NotNull NbtCompound target) throws ReadException, EofException, OomException,
																		 NbtParseException,
																		 FixedStack.FullStackException {
		FixedStack<Object> nestedTarget = this.nestedTarget;
		newTarget:
		for (; ; ) {
			int stackSize = nestedTarget.getSize();
			for (; ; ) {
				int type = di.expectByte();
				if (type == NbtType.tagEnd) {
					try {
						nestedTarget.pop(); // pop self
					} catch (FixedStack.EmptyStackException ex) {
						throw new IllegalStateException(ex);
					}

					Object parent = nestedTarget.tryPeek();
					if (parent == null) return null;
					if (parent instanceof ListOfLists) return (ListOfLists) parent;
					target = (NbtCompound) parent;
					continue newTarget;
				}
				String key = readString();
				switch (type) {
					case NbtType.tagByte:
						target.addByte(key, (byte) di.expectByte());
						break;
					case NbtType.tagShort:
						target.addShort(key, (short) di.expectShort());
						break;
					case NbtType.tagInt:
						target.addInt(key, di.expectInt());
						break;
					case NbtType.tagLong:
						target.addLong(key, di.expectLong());
						break;
					case NbtType.tagFloat:
						target.addFloat(key, di.expectFloat());
						break;
					case NbtType.tagDouble:
						target.addDouble(key, di.expectDouble());
						break;
					case NbtType.tagByteArray:
						target.addByteArray(key, readByteArray());
						break;
					case NbtType.tagIntArray:
						target.addIntArray(key, readIntArray());
						break;
					case NbtType.tagLongArray:
						target.addLongArray(key, readLongArray());
						break;
					case NbtType.tagString:
						target.addString(key, readString());
						break;
					case NbtType.tagList:
						NbtList list = readListValue(key);
						if (list == null) {
							try {
								return (ListOfLists) nestedTarget.peek();
							} catch (FixedStack.EmptyStackException ex) {
								throw new IllegalStateException(ex);
							}
						}
						target.addList(key, list);
						if (stackSize != nestedTarget.getSize()) {
							try {
								target = (NbtCompound) nestedTarget.peek();
							} catch (FixedStack.EmptyStackException ex) {
								throw new IllegalStateException(ex);
							}
							continue newTarget;
						}
						break;
					case NbtType.tagCompound:
						NbtCompound newDepth = new NbtCompound();
						nestedTarget.push(newDepth);
						target.addMap(key, newDepth);
						target = newDepth;
						continue newTarget;
					default:
						throw new NbtParseException.UnknownTagType(type);
				}
			}
		}
	}

	@NotNull
	private NbtCompound readListEntries(@NotNull ListOfLists target) throws ReadException, EofException, OomException,
																			NbtParseException,
																			FixedStack.FullStackException {
		FixedStack<Object> nestedTarget = this.nestedTarget;
		newTarget:
		for (; ; ) {
			@NotNull NbtList[] array = target.array;
			int len = array.length;
			int stackSize = nestedTarget.getSize();
			for (; ; ) {
				if (target.nextIdx >= len) {
					Object parent;

					try {
						nestedTarget.pop(); // pop self
						parent = nestedTarget.peek();
					} catch (FixedStack.EmptyStackException ex) {
						throw new IllegalStateException(ex);
					}
					if (parent instanceof NbtCompound) {
						assert target.key != null;

						NbtCompound parentAsMap = (NbtCompound) parent;
						parentAsMap.addList(target.key, target.result);
						return parentAsMap;
					}
					assert target.key == null;

					ListOfLists parentAsList = (ListOfLists) parent;
					parentAsList.array[parentAsList.nextIdx++] = target.result;
					target = parentAsList;
					continue newTarget;
				}

				NbtList list = readListValue(null);
				if (list == null) {
					try {
						target = (ListOfLists) nestedTarget.peek();
					} catch (FixedStack.EmptyStackException ex) {
						throw new IllegalStateException(ex);
					}
					continue newTarget;
				}
				if (stackSize != nestedTarget.getSize()) {
					try {
						return (NbtCompound) nestedTarget.peek();
					} catch (FixedStack.EmptyStackException ex) {
						throw new IllegalStateException(ex);
					}
				}
				array[target.nextIdx++] = list;
			}
		}
	}

	/**
	 * Reads a NBT List value if it is not nested (List of Lists or List of Maps).
	 * <p>If the parsed NBT List is a List of Lists, it will return null, and a ListOfLists target will be added to
	 * the top of the stack
	 * <p>If the parsed NBT List is a List of Maps, it will return a List with empty NbtCompound's. Those empty
	 * NbtCompound's will be added to the top of the stack, so that they can be parsed
	 */
	@Nullable
	private NbtList readListValue(@Nullable String key) throws ReadException, EofException, OomException,
															   NbtParseException, FixedStack.FullStackException {
		int type = di.expectByte();
		int size = readArraySize();
		if (size == 0) return NbtList.EMPTY_LIST;
		switch (type) {
			case NbtType.tagEnd:
				throw new NbtParseException.UnexpectedTagType(null, NbtType.tagEnd);
			case NbtType.tagByte:
				return NbtList.create(di.expectByteArray(size));
			case NbtType.tagShort:
				return NbtList.create(di.expectShortArray(size));
			case NbtType.tagInt:
				return NbtList.create(di.expectIntArray(size));
			case NbtType.tagLong:
				return NbtList.create(di.expectLongArray(size));
			case NbtType.tagFloat:
				return NbtList.create(di.expectFloatArray(size));
			case NbtType.tagDouble:
				return NbtList.create(di.expectDoubleArray(size));
			case NbtType.tagByteArray:
				return NbtList.create(readGenericArray(size, byte[][]::new, this::readByteArray));
			case NbtType.tagIntArray:
				return NbtList.create(readGenericArray(size, int[][]::new, this::readIntArray));
			case NbtType.tagLongArray:
				return NbtList.create(readGenericArray(size, long[][]::new, this::readLongArray));
			case NbtType.tagString:
				return NbtList.create(readGenericArray(size, String[]::new, this::readString));
			case NbtType.tagList:
				nestedTarget.push(OomAware.tryRun(oomAware, () -> new ListOfLists(key, size)));
				return null;
			case NbtType.tagCompound:
				NbtCompound[] maps = readGenericArray(size, NbtCompound[]::new, () ->
					// Wrap in OomAware.tryRun because there may be a lot of instances
					OomAware.tryRun(oomAware, NbtCompound::new));
				// False positive warning by IntelliJ, since it the compiler
				// will complain if it is not cast to an Object[]
				@SuppressWarnings("UnnecessaryLocalVariable")
				Object[] mapsAsObjects = maps;
				nestedTarget.pushAll(mapsAsObjects);
				return NbtList.create(maps);
			default:
				throw new NbtParseException.UnknownTagType(type);
		}
	}

	@NotNull
	private String readString() throws ReadException, EofException, OomException, NbtParseException.InvalidString {
		try {
			return di.expectModifiedUtf8();
		} catch (CheckedBigEndianDataInput.ModifiedUtf8DataFormatException ex) {
			throw new NbtParseException.InvalidString(ex);
		}
	}

	private int readArraySize() throws ReadException, EofException, NbtParseException.NegativeArraySize, OomException {
		int size = di.expectInt();
		if (size < 0) throw new NbtParseException.NegativeArraySize(size);
		if (size > GrowableArray.MAX_ARRAY_SIZE) throw OomException.INSTANCE;
		return size;
	}

	private byte[] readByteArray() throws ReadException, EofException, OomException, NbtParseException {
		return di.expectByteArray(readArraySize());
	}

	private int[] readIntArray() throws ReadException, EofException, OomException, NbtParseException {
		return di.expectIntArray(readArraySize());
	}

	private long[] readLongArray() throws ReadException, EofException, OomException, NbtParseException {
		return di.expectLongArray(readArraySize());
	}


	@FunctionalInterface
	private interface ReadGeneric<T, ReadException extends Throwable> {
		@NotNull
		T read() throws ReadException, EofException, OomException, NbtParseException;
	}

	private <T> T[] readGenericArray(int len, @NotNull IntFunction<T @NotNull []> genArray,
									 @NotNull ReadGeneric<T, ReadException> read)
		throws ReadException, EofException, OomException, NbtParseException {

		T[] array = OomAware.tryRun(oomAware, () -> genArray.apply(len));
		assert array.length == len;
		for (int i = 0; i < len; ++i)
			array[i] = read.read();
		return array;
	}

	private static final class ListOfLists {
		private final @NotNull NbtList result;
		private final @Nullable String key;

		private final @NotNull NbtList @NotNull [] array;
		private int nextIdx;

		private ListOfLists(@Nullable String key, int size) {
			this.key = key;
			this.array = new NbtList[size];
			this.result = NbtList.create(array);
		}
	}
}
