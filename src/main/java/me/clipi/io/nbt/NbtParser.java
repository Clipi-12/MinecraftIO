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
import me.clipi.io.nbt.exceptions.NbtKeyNotFoundException;
import me.clipi.io.nbt.exceptions.NbtParseException;
import me.clipi.io.nbt.schema.*;
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
			NbtCompound root = new NbtCompound(null);
			try {
				// Intentionally hide the rest of NbtCompound's methods in order to not expose them to the Schema
				@SuppressWarnings("FunctionalExpressionCanBeFolded")
				OomAware rootAsOnlyOomAware = root::trySaveFromOom;

				NbtCompoundSchema rootValueSchema = nonNullSchema(schema.schemaForRootValue(name, rootAsOnlyOomAware));
				di.setOomAware(oomAware = rootAsOnlyOomAware);
				readRootValue(rootValueSchema, root);
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

	private void readRootValue(@NotNull NbtCompoundSchema schema, @NotNull NbtCompound root)
		throws ReadException, EofException, OomException, NbtParseException, FixedStack.FullStackException {
		FixedStack<ParsingTarget> nestedTarget = this.nestedTarget;
		CompoundTarget target = new CompoundTarget(root, schema);
		nestedTarget.push(target);
		for (; ; ) {
			ListOfListsTarget nextTarget = readMapEntry(target);
			if (nextTarget == null) return;
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
					try {
						if (schema.deniesFinishedCompound(target))
							throw new NbtParseException.IncorrectSchema();
					} catch (NbtKeyNotFoundException ignored) {
						throw new NbtParseException.IncorrectSchema();
					}

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
						if (target.indexForKeyWithTypeOrNeg(key, NbtType.tagByte) >= 0)
							throw new NbtParseException.DuplicatedKey(key, target);
						if (schema.deniesByte(key)) throw new NbtParseException.IncorrectSchema();
						target.collisionUnsafeAddByte(key, (byte) di.expectByte());
						break;
					case NbtType.tagShort:
						if (target.indexForKeyWithTypeOrNeg(key, NbtType.tagShort) >= 0)
							throw new NbtParseException.DuplicatedKey(key, target);
						if (schema.deniesShort(key)) throw new NbtParseException.IncorrectSchema();
						target.collisionUnsafeAddShort(key, (short) di.expectShort());
						break;
					case NbtType.tagInt:
						if (target.indexForKeyWithTypeOrNeg(key, NbtType.tagInt) >= 0)
							throw new NbtParseException.DuplicatedKey(key, target);
						if (schema.deniesInt(key)) throw new NbtParseException.IncorrectSchema();
						target.collisionUnsafeAddInt(key, di.expectInt());
						break;
					case NbtType.tagLong:
						if (target.indexForKeyWithTypeOrNeg(key, NbtType.tagLong) >= 0)
							throw new NbtParseException.DuplicatedKey(key, target);
						if (schema.deniesLong(key)) throw new NbtParseException.IncorrectSchema();
						target.collisionUnsafeAddLong(key, di.expectLong());
						break;
					case NbtType.tagFloat:
						if (target.indexForKeyWithTypeOrNeg(key, NbtType.tagFloat) >= 0)
							throw new NbtParseException.DuplicatedKey(key, target);
						if (schema.deniesFloat(key)) throw new NbtParseException.IncorrectSchema();
						target.collisionUnsafeAddFloat(key, di.expectFloat());
						break;
					case NbtType.tagDouble:
						if (target.indexForKeyWithTypeOrNeg(key, NbtType.tagDouble) >= 0)
							throw new NbtParseException.DuplicatedKey(key, target);
						if (schema.deniesDouble(key)) throw new NbtParseException.IncorrectSchema();
						target.collisionUnsafeAddDouble(key, di.expectDouble());
						break;
					case NbtType.tagByteArray: {
						if (target.indexForKeyWithTypeOrNeg(key, NbtType.tagByteArray) >= 0)
							throw new NbtParseException.DuplicatedKey(key, target);
						target.collisionUnsafeAddByteArray(key, readByteArray(schema, key));
						break;
					}
					case NbtType.tagIntArray: {
						if (target.indexForKeyWithTypeOrNeg(key, NbtType.tagIntArray) >= 0)
							throw new NbtParseException.DuplicatedKey(key, target);
						target.collisionUnsafeAddIntArray(key, readIntArray(schema, key));
						break;
					}
					case NbtType.tagLongArray: {
						if (target.indexForKeyWithTypeOrNeg(key, NbtType.tagLongArray) >= 0)
							throw new NbtParseException.DuplicatedKey(key, target);
						target.collisionUnsafeAddLongArray(key, readLongArray(schema, key));
						break;
					}
					case NbtType.tagString: {
						if (target.indexForKeyWithTypeOrNeg(key, NbtType.tagString) >= 0)
							throw new NbtParseException.DuplicatedKey(key, target);
						target.collisionUnsafeAddString(key, readString(schema, key));
						break;
					}
					case NbtType.tagList: {
						if (target.indexForKeyWithTypeOrNeg(key, NbtType.tagList) >= 0)
							throw new NbtParseException.DuplicatedKey(key, target);
						NbtList list = readListValue(key, schema);
						if (list == null) {
							try {
								return (ListOfListsTarget) nestedTarget.peek();
							} catch (FixedStack.EmptyStackException ex) {
								throw new IllegalStateException(ex);
							}
						}
						target.collisionUnsafeAddList(key, list);
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
						if (target.indexForKeyWithTypeOrNeg(key, NbtType.tagCompound) >= 0)
							throw new NbtParseException.DuplicatedKey(key, target);
						NbtCompound newDepth = OomAware.tryRun(oomAware, () -> {
							try {
								return new NbtCompound(oomAware);
							} catch (OomException ex) {
								throw new OutOfMemoryError();
							}
						});
						NbtCompoundSchema newSchema = nonNullSchema(schema.schemaForCompound(key));
						nestedTarget.push(OomAware.tryRun(oomAware, () -> new CompoundTarget(newDepth, newSchema)));
						target.collisionUnsafeAddCompound(key, newDepth);
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
						parentAsMap.compound.collisionUnsafeAddList(target.key, target.result);
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
		NbtCompound[] maps = readGenericArray(len, NbtCompound[]::new, i ->
			// Wrap in OomAware.tryRun because there may be a lot of instances
			OomAware.tryRun(oomAwareOnlyFirstTimeCalled, () -> {
				try {
					return new NbtCompound(oomAwareOnlyFirstTimeCalled);
				} catch (OomException ex) {
					throw new OutOfMemoryError();
				}
			}));
		CompoundTarget[] targets = readGenericArray(
			len, CompoundTarget[]::new, i -> {
				NbtCompoundSchema compoundSchema = nonNullSchema(schema.schemaForCompound(i));
				return OomAware.tryRun(oomAwareOnlyFirstTimeCalled, () -> new CompoundTarget(maps[i], compoundSchema));
			}
		);
		nestedTarget.pushAll(targets);
		return OomAware.tryRun(oomAwareOnlyFirstTimeCalled, () -> NbtList.create(maps));
	}

	/**
	 * Reads a NBT List value if it is not nested (List of Lists or List of Maps).
	 * <p>If the parsed NBT List is a List of Lists, it will return null, and a ListOfListsTarget target will be
	 * added to the top of the stack.
	 * <p>If the parsed NBT List is a List of Maps, it will return a List with empty NbtCompound's. Those empty
	 * NbtCompound's will be added to the top of the stack as CompoundTarget's, so that they can be parsed.
	 */
	@Nullable
	private NbtList readListValue(int index, @NotNull NbtListOfListsSchema parentSchema)
		throws ReadException, EofException, OomException, NbtParseException, FixedStack.FullStackException {
		int type = di.expectByte();
		int len = readArrayLen();
		if (len == 0) {
			if (parentSchema.deniesEmptyList(index)) throw new NbtParseException.IncorrectSchema();
			return NbtList.EMPTY_LIST;
		}
		switch (type) {
			case NbtType.tagEnd:
				throw new NbtParseException.UnexpectedTagType(null, NbtType.tagEnd);
			case NbtType.tagByte: {
				if (parentSchema.deniesByteList(index, len)) throw new NbtParseException.IncorrectSchema();
				byte[] list = di.expectByteArray(len);
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagShort: {
				if (parentSchema.deniesShortList(index, len)) throw new NbtParseException.IncorrectSchema();
				short[] list = di.expectShortArray(len);
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagInt: {
				if (parentSchema.deniesIntList(index, len)) throw new NbtParseException.IncorrectSchema();
				int[] list = di.expectIntArray(len);
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagLong: {
				if (parentSchema.deniesLongList(index, len)) throw new NbtParseException.IncorrectSchema();
				long[] list = di.expectLongArray(len);
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagFloat: {
				if (parentSchema.deniesFloatList(index, len)) throw new NbtParseException.IncorrectSchema();
				float[] list = di.expectFloatArray(len);
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagDouble: {
				if (parentSchema.deniesDoubleList(index, len)) throw new NbtParseException.IncorrectSchema();
				double[] list = di.expectDoubleArray(len);
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagByteArray: {
				NbtListOfByteArraysSchema schema = nonNullSchema(parentSchema.schemaForListOfByteArrays(index, len));
				byte[][] list = readGenericArray(len, byte[][]::new, i -> readByteArray(schema, i));
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagIntArray: {
				NbtListOfIntArraysSchema schema = nonNullSchema(parentSchema.schemaForListOfIntArrays(index, len));
				int[][] list = readGenericArray(len, int[][]::new, i -> readIntArray(schema, i));
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagLongArray: {
				NbtListOfLongArraysSchema schema = nonNullSchema(parentSchema.schemaForListOfLongArrays(index, len));
				long[][] list = readGenericArray(len, long[][]::new, i -> readLongArray(schema, i));
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagString: {
				NbtListOfStringsSchema schema = nonNullSchema(parentSchema.schemaForListOfStrings(index, len));
				String[] list = readGenericArray(len, String[]::new, i -> readString(schema, i));
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
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
			if (parentSchema.deniesEmptyList(key)) throw new NbtParseException.IncorrectSchema();
			return NbtList.EMPTY_LIST;
		}
		switch (type) {
			case NbtType.tagEnd:
				throw new NbtParseException.UnexpectedTagType(null, NbtType.tagEnd);
			case NbtType.tagByte: {
				if (parentSchema.deniesByteList(key, len)) throw new NbtParseException.IncorrectSchema();
				byte[] list = di.expectByteArray(len);
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagShort: {
				if (parentSchema.deniesShortList(key, len)) throw new NbtParseException.IncorrectSchema();
				short[] list = di.expectShortArray(len);
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagInt: {
				if (parentSchema.deniesIntList(key, len)) throw new NbtParseException.IncorrectSchema();
				int[] list = di.expectIntArray(len);
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagLong: {
				if (parentSchema.deniesLongList(key, len)) throw new NbtParseException.IncorrectSchema();
				long[] list = di.expectLongArray(len);
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagFloat: {
				if (parentSchema.deniesFloatList(key, len)) throw new NbtParseException.IncorrectSchema();
				float[] list = di.expectFloatArray(len);
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagDouble: {
				if (parentSchema.deniesDoubleList(key, len)) throw new NbtParseException.IncorrectSchema();
				double[] list = di.expectDoubleArray(len);
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagByteArray: {
				NbtListOfByteArraysSchema schema = nonNullSchema(parentSchema.schemaForListOfByteArrays(key, len));
				byte[][] list = readGenericArray(len, byte[][]::new, i -> readByteArray(schema, i));
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagIntArray: {
				NbtListOfIntArraysSchema schema = nonNullSchema(parentSchema.schemaForListOfIntArrays(key, len));
				int[][] list = readGenericArray(len, int[][]::new, i -> readIntArray(schema, i));
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagLongArray: {
				NbtListOfLongArraysSchema schema = nonNullSchema(parentSchema.schemaForListOfLongArrays(key, len));
				long[][] list = readGenericArray(len, long[][]::new, i -> readLongArray(schema, i));
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
			case NbtType.tagString: {
				NbtListOfStringsSchema schema = nonNullSchema(parentSchema.schemaForListOfStrings(key, len));
				String[] list = readGenericArray(len, String[]::new, i -> readString(schema, i));
				return OomAware.tryRun(oomAware, () -> NbtList.create(list));
			}
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
	private String readString(@NotNull NbtCompoundSchema schema, @NotNull String key)
		throws ReadException, EofException, OomException, NbtParseException {
		int stringLen = di.expectShort();
		if (schema.deniesString(key, stringLen)) throw new NbtParseException.IncorrectSchema();
		try {
			return di.expectModifiedUtf8((short) stringLen);
		} catch (CheckedBigEndianDataInput.ModifiedUtf8DataFormatException ex) {
			throw new NbtParseException.InvalidString(ex);
		}
	}

	@NotNull
	private String readString(@NotNull NbtListOfStringsSchema schema, int index)
		throws ReadException, EofException, OomException, NbtParseException {
		int stringLen = di.expectShort();
		if (schema.deniesString(index, stringLen)) throw new NbtParseException.IncorrectSchema();
		try {
			return di.expectModifiedUtf8((short) stringLen);
		} catch (CheckedBigEndianDataInput.ModifiedUtf8DataFormatException ex) {
			throw new NbtParseException.InvalidString(ex);
		}
	}

	private int readArrayLen() throws ReadException, EofException, NbtParseException.InvalidDataStructureSize {
		int len = di.expectInt();
		if (len < 0 | len > GrowableArray.MAX_ARRAY_SIZE) throw new NbtParseException.InvalidDataStructureSize(len);
		return len;
	}

	private byte @NotNull [] readByteArray(@NotNull NbtCompoundSchema schema, @NotNull String key)
		throws ReadException, EofException, OomException, NbtParseException {
		int arrayLen = readArrayLen();
		if (schema.deniesByteArray(key, arrayLen)) throw new NbtParseException.IncorrectSchema();
		return di.expectByteArray(arrayLen);
	}

	private byte @NotNull [] readByteArray(@NotNull NbtListOfByteArraysSchema schema, int index)
		throws ReadException, EofException, OomException, NbtParseException {
		int arrayLen = readArrayLen();
		if (schema.deniesByteArray(index, arrayLen)) throw new NbtParseException.IncorrectSchema();
		return di.expectByteArray(arrayLen);
	}

	private int @NotNull [] readIntArray(@NotNull NbtCompoundSchema schema, @NotNull String key)
		throws ReadException, EofException, OomException, NbtParseException {
		int arrayLen = readArrayLen();
		if (schema.deniesIntArray(key, arrayLen)) throw new NbtParseException.IncorrectSchema();
		return di.expectIntArray(arrayLen);
	}

	private int @NotNull [] readIntArray(@NotNull NbtListOfIntArraysSchema schema, int index)
		throws ReadException, EofException, OomException, NbtParseException {
		int arrayLen = readArrayLen();
		if (schema.deniesIntArray(index, arrayLen)) throw new NbtParseException.IncorrectSchema();
		return di.expectIntArray(arrayLen);
	}

	private long @NotNull [] readLongArray(@NotNull NbtCompoundSchema schema, @NotNull String key)
		throws ReadException, EofException, OomException, NbtParseException {
		int arrayLen = readArrayLen();
		if (schema.deniesLongArray(key, arrayLen)) throw new NbtParseException.IncorrectSchema();
		return di.expectLongArray(arrayLen);
	}

	private long @NotNull [] readLongArray(@NotNull NbtListOfLongArraysSchema schema, int index)
		throws ReadException, EofException, OomException, NbtParseException {
		int arrayLen = readArrayLen();
		if (schema.deniesLongArray(index, arrayLen)) throw new NbtParseException.IncorrectSchema();
		return di.expectLongArray(arrayLen);
	}


	@FunctionalInterface
	private interface ReadGeneric<T, ReadException extends Throwable> {
		@NotNull
		T read(int index) throws ReadException, EofException, OomException, NbtParseException;
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
