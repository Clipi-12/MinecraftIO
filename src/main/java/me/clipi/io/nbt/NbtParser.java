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
import me.clipi.io.nbt.schema.NbtCompoundSchema;
import me.clipi.io.nbt.schema.NbtListOfCompoundsSchema;
import me.clipi.io.nbt.schema.NbtListOfListsSchema;
import me.clipi.io.nbt.schema.NbtRootSchema;
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
	 * FixedStack of objects that are either CompoundTarget or ListOfListsTarget.
	 * If the project used Java 17, this could be improved with sealed classes
	 *
	 * <p>The wiki says that <pre>Compound and List tags may not be nested beyond a depth of {@code 512}</pre>,
	 * but we allow for {@code 1024}, and an additional tag for the root
	 */
	private final FixedStack<ParsingTarget> nestedTarget = new FixedStack<>(ParsingTarget.class, 1025);
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
	public NbtRoot parseRoot() throws ReadException, OomException, NbtParseException {
		return parseRoot(NbtRootSchema.ALWAYS);
	}

	@NotNull
	private static <T> T nonNullSchema(@Nullable T schema) throws NbtParseException.IncorrectSchema {
		if (schema == null) throw new NbtParseException.IncorrectSchema();
		return schema;
	}

	@NotNull
	public NbtRoot parseRoot(@NotNull NbtRootSchema schema) throws ReadException, OomException, NbtParseException {
		try {
			di.expectedByteFail(NbtType.tagCompound, type -> {
				throw new NbtParseException.UnexpectedTagType(NbtType.Compound, type);
			});
			String name = readString();
			NbtCompoundSchema rootValueSchema = nonNullSchema(schema.schemaForCompound(name));
			NbtCompound root;
			try {
				root = readRootValue(rootValueSchema);
			} finally {
				di.setOomAware(oomAware = null);
				nestedTarget.clear();
			}
			di.expectEnd();
			return new NbtRoot(name, root);
		} catch (FixedStack.FullStackException ex) {
			throw new NbtParseException.InvalidDataStructureSize(ex.attemptedSize);
		} catch (NotEofException ex) {
			throw new NbtParseException.NotEofException(ex);
		} catch (EofException ex) {
			throw new NbtParseException.EofException(ex);
		} finally {
			closeCurrent();
		}
	}

	@NotNull
	private NbtCompound readRootValue(@NotNull NbtCompoundSchema schema)
		throws ReadException, EofException, OomException, NbtParseException, FixedStack.FullStackException {
		FixedStack<ParsingTarget> nestedTarget = this.nestedTarget;
		NbtCompound root = new NbtCompound();
		di.setOomAware(oomAware = root);
		CompoundTarget target = new CompoundTarget(root, schema);
		nestedTarget.push(target);
		for (; ; ) {
			ListOfListsTarget nextTarget = readMapEntry(target);
			if (nextTarget == null) return root;
			target = readListEntries(nextTarget);
		}
	}

	/**
	 * @return whether the root has been reached or a ListOfListsTarget target is on top of the stack
	 */
	@Nullable
	private ListOfListsTarget readMapEntry(@NotNull CompoundTarget targetAndSchema)
		throws ReadException, EofException, OomException, NbtParseException, FixedStack.FullStackException {
		FixedStack<ParsingTarget> nestedTarget = this.nestedTarget;
		NbtCompound target = targetAndSchema.compound;
		NbtCompoundSchema schema = targetAndSchema.schema;
		newTarget:
		for (; ; ) {
			int stackSize = nestedTarget.getSize();
			for (; ; ) {
				int type = di.expectByte();
				if (type == NbtType.tagEnd) {
					if (!schema.allowsSizeToBe(target.entries())) throw new NbtParseException.IncorrectSchema();

					try {
						nestedTarget.pop(); // pop self
					} catch (FixedStack.EmptyStackException ex) {
						throw new IllegalStateException(ex);
					}

					ParsingTarget parent = nestedTarget.tryPeek();
					if (parent == null) return null;
					if (parent instanceof ListOfListsTarget) return (ListOfListsTarget) parent;
					CompoundTarget parentAsCompound = (CompoundTarget) parent;
					target = parentAsCompound.compound;
					schema = parentAsCompound.schema;
					continue newTarget;
				}
				String key = readString();
				switch (type) {
					case NbtType.tagByte:
						if (!schema.allowsByte(key)) throw new NbtParseException.IncorrectSchema();
						target.addByte(key, (byte) di.expectByte());
						break;
					case NbtType.tagShort:
						if (!schema.allowsShort(key)) throw new NbtParseException.IncorrectSchema();
						target.addShort(key, (short) di.expectShort());
						break;
					case NbtType.tagInt:
						if (!schema.allowsInt(key)) throw new NbtParseException.IncorrectSchema();
						target.addInt(key, di.expectInt());
						break;
					case NbtType.tagLong:
						if (!schema.allowsLong(key)) throw new NbtParseException.IncorrectSchema();
						target.addLong(key, di.expectLong());
						break;
					case NbtType.tagFloat:
						if (!schema.allowsFloat(key)) throw new NbtParseException.IncorrectSchema();
						target.addFloat(key, di.expectFloat());
						break;
					case NbtType.tagDouble:
						if (!schema.allowsDouble(key)) throw new NbtParseException.IncorrectSchema();
						target.addDouble(key, di.expectDouble());
						break;
					case NbtType.tagByteArray: {
						int arrayLen = readArrayLen();
						if (!schema.allowsByteArray(key, arrayLen)) throw new NbtParseException.IncorrectSchema();
						target.addByteArray(key, di.expectByteArray(arrayLen));
						break;
					}
					case NbtType.tagIntArray: {
						int arrayLen = readArrayLen();
						if (!schema.allowsIntArray(key, arrayLen)) throw new NbtParseException.IncorrectSchema();
						target.addIntArray(key, di.expectIntArray(arrayLen));
						break;
					}
					case NbtType.tagLongArray: {
						int arrayLen = readArrayLen();
						if (!schema.allowsLongArray(key, arrayLen)) throw new NbtParseException.IncorrectSchema();
						target.addLongArray(key, di.expectLongArray(arrayLen));
						break;
					}
					case NbtType.tagString: {
						int stringLen = di.expectShort();
						if (!schema.allowsString(key, stringLen)) throw new NbtParseException.IncorrectSchema();
						target.addString(key, readString((short) stringLen));
						break;
					}
					case NbtType.tagList: {
						NbtList list = readListValue(key, schema);
						if (list == null) {
							try {
								return (ListOfListsTarget) nestedTarget.peek();
							} catch (FixedStack.EmptyStackException ex) {
								throw new IllegalStateException(ex);
							}
						}
						target.addList(key, list);
						if (stackSize != nestedTarget.getSize()) {
							try {
								CompoundTarget newTarget = (CompoundTarget) nestedTarget.peek();
								target = newTarget.compound;
								schema = newTarget.schema;
							} catch (FixedStack.EmptyStackException ex) {
								throw new IllegalStateException(ex);
							}
							continue newTarget;
						}
						break;
					}
					case NbtType.tagCompound: {
						NbtCompound newDepth = new NbtCompound();
						NbtCompoundSchema newSchema = nonNullSchema(schema.schemaForCompound(key));
						nestedTarget.push(new CompoundTarget(newDepth, newSchema));
						target.addMap(key, newDepth);
						target = newDepth;
						schema = newSchema;
						continue newTarget;
					}
					default:
						throw new NbtParseException.UnknownTagType(type);
				}
			}
		}
	}

	@NotNull
	private CompoundTarget readListEntries(@NotNull ListOfListsTarget target)
		throws ReadException, EofException, OomException, NbtParseException, FixedStack.FullStackException {
		FixedStack<ParsingTarget> nestedTarget = this.nestedTarget;
		newTarget:
		for (; ; ) {
			NbtListOfListsSchema schema = target.schema;
			@NotNull NbtList[] array = target.array;
			final int len = array.length, stackSize = nestedTarget.getSize();
			int i = target.nextIdx;
			for (; ; ) {
				if (i >= len) {
					ParsingTarget parent;

					try {
						nestedTarget.pop(); // pop self
						parent = nestedTarget.peek();
					} catch (FixedStack.EmptyStackException ex) {
						throw new IllegalStateException(ex);
					}
					if (parent instanceof CompoundTarget) {
						assert target.key != null;

						CompoundTarget parentAsMap = (CompoundTarget) parent;
						parentAsMap.compound.addList(target.key, target.result);
						return parentAsMap;
					}
					assert target.key == null;

					ListOfListsTarget parentAsList = (ListOfListsTarget) parent;
					parentAsList.array[parentAsList.nextIdx++] = target.result;
					target.nextIdx = i;
					target = parentAsList;
					continue newTarget;
				}

				NbtList list = readListValue(i, schema);
				if (list == null) {
					target.nextIdx = i;
					try {
						target = (ListOfListsTarget) nestedTarget.peek();
					} catch (FixedStack.EmptyStackException ex) {
						throw new IllegalStateException(ex);
					}
					continue newTarget;
				}
				if (stackSize != nestedTarget.getSize()) {
					try {
						return (CompoundTarget) nestedTarget.peek();
					} catch (FixedStack.EmptyStackException ex) {
						throw new IllegalStateException(ex);
					}
				}
				array[i++] = list;
			}
		}
	}

	private NbtList readListOfCompoundsValue(NbtListOfCompoundsSchema schema, int len)
		throws ReadException, OomException, EofException, FixedStack.FullStackException, NbtParseException {
		OomAware oomAwareOnlyFirstTimeCalled = new OomAware() {
			private boolean skipCall;

			@Override
			public void trySaveFromOom() {
				if (skipCall) return;
				skipCall = true;
				OomAware oomAware = NbtParser.this.oomAware;
				if (oomAware != null) oomAware.trySaveFromOom();
			}
		};
		NbtCompound[] maps = readGenericArray(len, NbtCompound[]::new, () ->
			// Wrap in OomAware.tryRun because there may be a lot of instances
			OomAware.tryRun(oomAwareOnlyFirstTimeCalled, NbtCompound::new));
		CompoundTarget[] targets = readGenericArray(
			len, CompoundTarget[]::new,
			new ReadGeneric<CompoundTarget, ReadException>() {
				@Override
				public @NotNull CompoundTarget read() {
					throw new AssertionError();
				}

				@Override
				public @NotNull CompoundTarget read(int i) throws OomException, NbtParseException {
					NbtCompoundSchema compoundSchema = nonNullSchema(schema.schemaForCompound(i));
					return OomAware.tryRun(oomAwareOnlyFirstTimeCalled,
										   () -> new CompoundTarget(maps[i], compoundSchema));
				}
			});
		nestedTarget.pushAll(targets);
		return NbtList.create(maps);
	}

	/**
	 * Reads a NBT List value if it is not nested (List of Lists or List of Maps).
	 * <p>If the parsed NBT List is a List of Lists, it will return null, and a ListOfListsTarget target will be
	 * added to the top of the stack.
	 * <p>If the parsed NBT List is a List of Maps, it will return a List with empty NbtCompound's. Those empty
	 * NbtCompound's will be added to the top of the stack as CompoundTarget's, so that they can be parsed.
	 */
	@Nullable
	private NbtList readListValue(int index, @NotNull NbtListOfListsSchema parentSchema) throws ReadException,
																								EofException,
																								OomException,
																								NbtParseException,
																								FixedStack.FullStackException {
		int type = di.expectByte();
		int len = readArrayLen();
		if (len == 0) {
			if (!parentSchema.allowsEmptyList(index)) throw new NbtParseException.IncorrectSchema();
			return NbtList.EMPTY_LIST;
		}
		switch (type) {
			case NbtType.tagEnd:
				throw new NbtParseException.UnexpectedTagType(null, NbtType.tagEnd);
			case NbtType.tagByte:
				if (!parentSchema.allowsByteList(index, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(di.expectByteArray(len));
			case NbtType.tagShort:
				if (!parentSchema.allowsShortList(index, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(di.expectShortArray(len));
			case NbtType.tagInt:
				if (!parentSchema.allowsIntList(index, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(di.expectIntArray(len));
			case NbtType.tagLong:
				if (!parentSchema.allowsLongList(index, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(di.expectLongArray(len));
			case NbtType.tagFloat:
				if (!parentSchema.allowsFloatList(index, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(di.expectFloatArray(len));
			case NbtType.tagDouble:
				if (!parentSchema.allowsDoubleList(index, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(di.expectDoubleArray(len));
			case NbtType.tagByteArray:
				if (!parentSchema.allowsByteArrayList(index, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(readGenericArray(len, byte[][]::new, this::readByteArray));
			case NbtType.tagIntArray:
				if (!parentSchema.allowsIntArrayList(index, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(readGenericArray(len, int[][]::new, this::readIntArray));
			case NbtType.tagLongArray:
				if (!parentSchema.allowsLongArrayList(index, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(readGenericArray(len, long[][]::new, this::readLongArray));
			case NbtType.tagString:
				if (!parentSchema.allowsStringList(index, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(readGenericArray(len, String[]::new, this::readString));
			case NbtType.tagList:
				NbtListOfListsSchema schema = nonNullSchema(parentSchema.schemaForListOfLists(index, len));
				nestedTarget.push(OomAware.tryRun(oomAware, () -> new ListOfListsTarget(null, len, schema)));
				return null;
			case NbtType.tagCompound:
				return readListOfCompoundsValue(nonNullSchema(parentSchema.schemaForListOfCompounds(index, len)), len);
			default:
				throw new NbtParseException.UnknownTagType(type);
		}
	}

	/**
	 * Reads a NBT List value if it is not nested (List of Lists or List of Maps).
	 * <p>If the parsed NBT List is a List of Lists, it will return null, and a ListOfListsTarget target will be
	 * added to the top of the stack.
	 * <p>If the parsed NBT List is a List of Maps, it will return a List with empty NbtCompound's. Those empty
	 * NbtCompound's will be added to the top of the stack as CompoundTarget's, so that they can be parsed.
	 */
	@Nullable
	private NbtList readListValue(@NotNull String key, @NotNull NbtCompoundSchema parentSchema)
		throws ReadException, EofException, OomException, NbtParseException, FixedStack.FullStackException {
		int type = di.expectByte();
		int len = readArrayLen();
		if (len == 0) {
			if (!parentSchema.allowsEmptyList(key)) throw new NbtParseException.IncorrectSchema();
			return NbtList.EMPTY_LIST;
		}
		switch (type) {
			case NbtType.tagEnd:
				throw new NbtParseException.UnexpectedTagType(null, NbtType.tagEnd);
			case NbtType.tagByte:
				if (!parentSchema.allowsByteList(key, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(di.expectByteArray(len));
			case NbtType.tagShort:
				if (!parentSchema.allowsShortList(key, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(di.expectShortArray(len));
			case NbtType.tagInt:
				if (!parentSchema.allowsIntList(key, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(di.expectIntArray(len));
			case NbtType.tagLong:
				if (!parentSchema.allowsLongList(key, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(di.expectLongArray(len));
			case NbtType.tagFloat:
				if (!parentSchema.allowsFloatList(key, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(di.expectFloatArray(len));
			case NbtType.tagDouble:
				if (!parentSchema.allowsDoubleList(key, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(di.expectDoubleArray(len));
			case NbtType.tagByteArray:
				if (!parentSchema.allowsByteArrayList(key, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(readGenericArray(len, byte[][]::new, this::readByteArray));
			case NbtType.tagIntArray:
				if (!parentSchema.allowsIntArrayList(key, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(readGenericArray(len, int[][]::new, this::readIntArray));
			case NbtType.tagLongArray:
				if (!parentSchema.allowsLongArrayList(key, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(readGenericArray(len, long[][]::new, this::readLongArray));
			case NbtType.tagString:
				if (!parentSchema.allowsStringList(key, len)) throw new NbtParseException.IncorrectSchema();
				return NbtList.create(readGenericArray(len, String[]::new, this::readString));
			case NbtType.tagList:
				NbtListOfListsSchema schema = nonNullSchema(parentSchema.schemaForListOfLists(key, len));
				nestedTarget.push(OomAware.tryRun(oomAware, () -> new ListOfListsTarget(key, len, schema)));
				return null;
			case NbtType.tagCompound:
				return readListOfCompoundsValue(nonNullSchema(parentSchema.schemaForListOfCompounds(key, len)), len);
			default:
				throw new NbtParseException.UnknownTagType(type);
		}
	}

	@NotNull
	private String readString() throws ReadException, EofException, OomException,
									   NbtParseException.InvalidString {
		try {
			return di.expectModifiedUtf8();
		} catch (CheckedBigEndianDataInput.ModifiedUtf8DataFormatException ex) {
			throw new NbtParseException.InvalidString(ex);
		}
	}

	@NotNull
	private String readString(short len) throws ReadException, EofException, OomException,
												NbtParseException.InvalidString {
		try {
			return di.expectModifiedUtf8(len);
		} catch (CheckedBigEndianDataInput.ModifiedUtf8DataFormatException ex) {
			throw new NbtParseException.InvalidString(ex);
		}
	}

	private int readArrayLen() throws ReadException, EofException, NbtParseException.InvalidDataStructureSize {
		int len = di.expectInt();
		if (len < 0 | len > GrowableArray.MAX_ARRAY_SIZE) throw new NbtParseException.InvalidDataStructureSize(len);
		return len;
	}

	private byte[] readByteArray() throws ReadException, EofException, OomException, NbtParseException {
		return di.expectByteArray(readArrayLen());
	}

	private int[] readIntArray() throws ReadException, EofException, OomException, NbtParseException {
		return di.expectIntArray(readArrayLen());
	}

	private long[] readLongArray() throws ReadException, EofException, OomException, NbtParseException {
		return di.expectLongArray(readArrayLen());
	}


	@FunctionalInterface
	private interface ReadGeneric<T, ReadException extends Throwable> {
		@NotNull
		T read() throws ReadException, EofException, OomException, NbtParseException;

		@NotNull
		default T read(int index) throws ReadException, EofException, OomException, NbtParseException {
			return read();
		}
	}

	private <T> T[] readGenericArray(int len, @NotNull IntFunction<T @NotNull []> genArray,
									 @NotNull ReadGeneric<T, ReadException> read)
		throws ReadException, EofException, OomException, NbtParseException {

		T[] array = OomAware.tryRun(oomAware, () -> genArray.apply(len));
		assert array.length == len;
		for (int i = 0; i < len; ++i)
			array[i] = read.read(i);
		return array;
	}

	private interface ParsingTarget {
	}

	private static final class CompoundTarget implements ParsingTarget {
		private final @NotNull NbtCompoundSchema schema;
		private final @NotNull NbtCompound compound;

		private CompoundTarget(@NotNull NbtCompound compound, @NotNull NbtCompoundSchema schema) {
			this.schema = schema;
			this.compound = compound;
		}
	}

	private static final class ListOfListsTarget implements ParsingTarget {
		private final @NotNull NbtListOfListsSchema schema;

		private final @NotNull NbtList result;
		private final @Nullable String key;

		private final @NotNull NbtList @NotNull [] array;
		private int nextIdx;

		private ListOfListsTarget(@Nullable String key, int len, @NotNull NbtListOfListsSchema schema) {
			this.schema = schema;
			this.key = key;
			this.array = new NbtList[len];
			this.result = NbtList.create(array);
		}
	}
}
