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
import me.clipi.io.util.function.CheckedConsumer;
import me.clipi.io.util.function.CheckedRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

/**
 * @see <a href="https://minecraft.wiki/w/NBT_format">NBT format</a>
 */
public class NbtParser<ReadException extends Exception> implements AutoCloseable {
	private final CheckedBigEndianDataInput<ReadException> di;
	/**
	 * The wiki says that <pre>Compound and List tags may not be nested beyond a depth of {@code 512}</pre>,
	 * but we allow for {@code 1024}, and an additional tag for the root
	 */
	static final int NESTED_MAX_DEPTH = 1025;
	/**
	 * FixedStack of objects that are either CompoundTarget or ListOfListsTarget.
	 * If the project used Java 17, this could be improved with sealed classes
	 */
	private final FixedStack<ParsingTarget> nestedTarget = new FixedStack<>(ParsingTarget.class, NESTED_MAX_DEPTH);
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

	private static class AnyRoot implements NbtRootSchema<SaveCompoundSchema> {
		private String rootName;

		@Override
		public @Nullable SaveCompoundSchema schemaForRootValue(@NotNull String rootName, @NotNull OomAware oomAware) throws OomException {
			this.rootName = rootName;
			return SaveCompoundSchema.create(oomAware);
		}
	}

	@NotNull
	public NbtRoot parseRoot() throws ReadException, OomException, NbtParseException {
		AnyRoot anyRoot = new AnyRoot();
		NbtCompound rootValue = parseRoot(anyRoot).compound;
		return rootValue.tryRun(() -> new NbtRoot(anyRoot.rootName, rootValue));
	}

	@NotNull
	private static <T> T nonNullSchema(@NotNull Object parentSchema, @Nullable T schema) throws NbtParseException.IncorrectSchema {
		if (schema == null) throw new NbtParseException.IncorrectSchema(parentSchema);
		return schema;
	}

	@NotNull
	public <T extends NbtCompoundSchema> T parseRoot(@NotNull NbtRootSchema<T> schema)
		throws ReadException, OomException, NbtParseException {
		try {
			di.expectedByteFail(NbtType.tagCompound, type -> {
				throw new NbtParseException.UnexpectedTagType(NbtType.Compound, type);
			});
			String name = readString();
			final OomAware[] delegatedOomAware = { null };
			T rootValueSchema = nonNullSchema(schema, schema.schemaForRootValue(name, () -> {
				OomAware oomAware = delegatedOomAware[0];
				if (oomAware != null) oomAware.trySaveFromOom();
			}));
			ValuelessNbtCompound root = rootValueSchema instanceof SaveCompoundSchema ?
				((SaveCompoundSchema) rootValueSchema).compound :
				ValuelessNbtCompound.create(null);
			delegatedOomAware[0] = root;
			try {
				di.setOomAware(oomAware = root);
				readRootValue(rootValueSchema, root);
			} finally {
				di.setOomAware(oomAware = null);
				nestedTarget.clear();
			}
			di.expectEnd();
			return rootValueSchema;
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

	private void readRootValue(@NotNull NbtCompoundSchema schema, @NotNull ValuelessNbtCompound root)
		throws ReadException, EofException, OomException, NbtParseException, FixedStack.FullStackException {
		FixedStack<ParsingTarget> nestedTarget = this.nestedTarget;
		CompoundTarget target = OomAware.tryRun(oomAware, () -> new CompoundTarget(root, schema));
		nestedTarget.push(target);
		for (; ; ) {
			ListOfListsTarget nextTarget = readMapEntries(target);
			if (nextTarget == null) return;
			target = readListEntries(nextTarget);
		}
	}

	/**
	 * Reads NBT Compound entries without recursion
	 *
	 * @return whether the root has been reached or a ListOfListsTarget target is on top of the stack
	 */
	@Nullable
	private ListOfListsTarget readMapEntries(@NotNull CompoundTarget targetAndSchema)
		throws ReadException, EofException, OomException, NbtParseException, FixedStack.FullStackException {
		FixedStack<ParsingTarget> nestedTarget = this.nestedTarget;
		assert nestedTarget.tryPeek() == targetAndSchema;

		ValuelessNbtCompound target = targetAndSchema.compound;
		NbtCompoundSchema schema = targetAndSchema.schema;
		newTarget:
		for (; ; ) {
			int stackSize = nestedTarget.getSize();
			for (; ; ) {
				int type = di.expectByte();
				if (type == NbtType.tagEnd) {
					try {
						if (schema.deniesFinishedCompound())
							throw new NbtParseException.IncorrectSchema(schema);
					} catch (NbtParseException.IncorrectSchema ex) {
						throw ex;
					} catch (NbtParseException | NbtKeyNotFoundException cause) {
						throw new NbtParseException.IncorrectSchema(cause);
					}

					if (targetAndSchema.advanceIsFinished()) {
						try {
							nestedTarget.pop(); // pop self
						} catch (FixedStack.EmptyStackException ex) {
							throw new IllegalStateException(ex);
						}

						ParsingTarget parent = nestedTarget.tryPeek();
						if (parent == null) return null;
						if (parent instanceof ListOfListsTarget) return (ListOfListsTarget) parent;
						targetAndSchema = (CompoundTarget) parent;
					}
					target = targetAndSchema.compound;
					schema = targetAndSchema.schema;
					continue newTarget;
				}
				String key = readString();
				if (target.containsKey(key)) throw new NbtParseException.DuplicatedKey(key, schema);
				switch (type) {
					case NbtType.tagByte: {
						int value = di.expectByte();
						if (schema.deniesByte(key, value)) throw new NbtParseException.IncorrectSchema(schema);
						target.collisionUnsafeAddByte(key, (byte) value);
						break;
					}
					case NbtType.tagShort: {
						int value = di.expectShort();
						if (schema.deniesShort(key, value)) throw new NbtParseException.IncorrectSchema(schema);
						target.collisionUnsafeAddShort(key, (short) value);
						break;
					}
					case NbtType.tagInt: {
						int value = di.expectInt();
						if (schema.deniesInt(key, value)) throw new NbtParseException.IncorrectSchema(schema);
						target.collisionUnsafeAddInt(key, value);
						break;
					}
					case NbtType.tagLong: {
						long value = di.expectLong();
						if (schema.deniesLong(key, value)) throw new NbtParseException.IncorrectSchema(schema);
						target.collisionUnsafeAddLong(key, value);
						break;
					}
					case NbtType.tagFloat: {
						float value = di.expectFloat();
						if (schema.deniesFloat(key, value)) throw new NbtParseException.IncorrectSchema(schema);
						target.collisionUnsafeAddFloat(key, value);
						break;
					}
					case NbtType.tagDouble: {
						double value = di.expectDouble();
						if (schema.deniesDouble(key, value)) throw new NbtParseException.IncorrectSchema(schema);
						target.collisionUnsafeAddDouble(key, value);
						break;
					}
					case NbtType.tagByteArray:
						target.collisionUnsafeAddByteArray(key, readByteArray(schema, key));
						break;
					case NbtType.tagIntArray:
						target.collisionUnsafeAddIntArray(key, readIntArray(schema, key));
						break;
					case NbtType.tagLongArray:
						target.collisionUnsafeAddLongArray(key, readLongArray(schema, key));
						break;
					case NbtType.tagString:
						target.collisionUnsafeAddString(key, readString(schema, key));
						break;
					case NbtType.tagList: {
						ValuelessNbtCompound finalTarget = target;
						if (readListValue(key, schema, list -> finalTarget.collisionUnsafeAddList(key, list))) {
							if (stackSize == nestedTarget.getSize()) break;
							try {
								targetAndSchema = (CompoundTarget) nestedTarget.peek();
								target = targetAndSchema.compound;
								schema = targetAndSchema.schema;
							} catch (FixedStack.EmptyStackException ex) {
								throw new IllegalStateException(ex);
							}
							continue newTarget;
						} else {
							try {
								return (ListOfListsTarget) nestedTarget.peek();
							} catch (FixedStack.EmptyStackException ex) {
								throw new IllegalStateException(ex);
							}
						}
					}
					case NbtType.tagCompound: {
						NbtCompoundSchema newSchema = nonNullSchema(schema, schema.schemaForCompound(key));
						ValuelessNbtCompound newDepth = newSchema instanceof SaveCompoundSchema ?
							((SaveCompoundSchema) newSchema).compound :
							ValuelessNbtCompound.create(oomAware);
						nestedTarget.push(targetAndSchema = OomAware.tryRun(oomAware, () ->
							new CompoundTarget(newDepth, newSchema)));
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

	/**
	 * Reads NBT List entries without recursion
	 */
	@NotNull
	private CompoundTarget readListEntries(@NotNull ListOfListsTarget target)
		throws ReadException, EofException, OomException, NbtParseException, FixedStack.FullStackException {
		FixedStack<ParsingTarget> nestedTarget = this.nestedTarget;
		assert nestedTarget.tryPeek() == target;

		newTarget:
		for (; ; ) {
			NbtListOfListsSchema schema = target.schema;
			@NotNull NbtList[] array = target.array;
			final int len = array.length, stackSize = nestedTarget.getSize();
			int i = target.savedIndex;
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
					parentAsList.array[parentAsList.savedIndex - 1] = target.result;
					target = parentAsList;
					continue newTarget;
				}

				int finalI = i;
				++i;
				if (readListValue(i, schema, list -> array[finalI] = list)) {
					if (stackSize != nestedTarget.getSize()) {
						target.savedIndex = i;
						try {
							return (CompoundTarget) nestedTarget.peek();
						} catch (FixedStack.EmptyStackException ex) {
							throw new IllegalStateException(ex);
						}
					}
				} else {
					ListOfListsTarget child;
					try {
						child = (ListOfListsTarget) nestedTarget.peek();
					} catch (FixedStack.EmptyStackException ex) {
						throw new IllegalStateException(ex);
					}

					target.savedIndex = i;
					target = child;
					continue newTarget;
				}
			}
		}
	}

	/**
	 * An array of {@link ValuelessNbtCompound} will be created, and each of those will be added to the top of the
	 * stack as {@link CompoundTarget}s, so that they can be parsed.
	 * <p>If the corresponding {@link NbtListOfCompoundsSchema} is an instance of
	 * {@link SaveCompoundSchema.ListOfCompounds}, the mentioned {@link ValuelessNbtCompound}s will be
	 * {@link NbtCompound}s, and a {@link NbtList} of them will be handled when all finish parsing. Otherwise, an
	 * empty list will be handled when all have finished parsing.
	 */
	private void readListOfCompoundsValue(@NotNull NbtListOfCompoundsSchema schema, int len,
										  @NotNull CheckedConsumer<NbtList, OomException> onFinish)
		throws ReadException, OomException, EofException, FixedStack.FullStackException, NbtParseException {
		NbtList result;
		ValuelessNbtCompound[] valuelessCompounds;
		if (schema instanceof SaveCompoundSchema.ListOfCompounds) {
			NbtCompound[] compounds = ((SaveCompoundSchema.ListOfCompounds) schema).array;
			valuelessCompounds = compounds;
			result = NbtList.emptyUnsafeCreate(oomAware, compounds);
		} else {
			valuelessCompounds = readGenericArray(len, ValuelessNbtCompound[]::new, i ->
				// Wrap in OomAware.tryRun because there may be a lot of instances
				ValuelessNbtCompound.create(oomAware)
			);
			result = NbtList.EMPTY_LIST;
		}
		ListOfCompoundsTarget newTarget =
			OomAware.tryRun(oomAware, () -> new ListOfCompoundsTarget(valuelessCompounds, schema,
																	  () -> onFinish.accept(result)));
		nestedTarget.push(newTarget);
		if (newTarget.advanceIsFinished()) throw new AssertionError(
			"Passed len was 0 (sanity check: actual len is = " + len + ")");
	}

	/**
	 * Reads a NBT List value if it is not nested (List of Lists or List of Maps) and returns {@code true}.
	 * <p>If the parsed NBT List is a List of Lists, it will return {@code false}, and a {@link ListOfListsTarget}
	 * target will be added to the top of the stack.
	 * <p>If the parsed NBT List is a List of Maps, an array of {@link ValuelessNbtCompound} will be created, and
	 * each of those will be added to the top of the stack as {@link CompoundTarget}s, so that they can be parsed.
	 * If the corresponding {@link NbtListOfCompoundsSchema} is an instance of
	 * {@link SaveCompoundSchema.ListOfCompounds}, the mentioned {@link ValuelessNbtCompound}s will be
	 * {@link NbtCompound}s, and a {@link NbtList} of them will be handled when all finish parsing. Otherwise, an
	 * empty list will be handled when all have finished parsing.
	 */
	private boolean readListValue(int index, @NotNull NbtListOfListsSchema parentSchema,
								  @NotNull CheckedConsumer<@NotNull NbtList, OomException> handleList)
		throws ReadException, EofException, OomException, NbtParseException, FixedStack.FullStackException {
		int type = di.expectByte();
		int len = readArrayLen();
		if (len == 0) {
			if (parentSchema.deniesEmptyList(index)) throw new NbtParseException.IncorrectSchema(parentSchema);
			handleList.accept(NbtList.EMPTY_LIST);
			return true;
		}
		NbtList listToBeHandled;
		switch (type) {
			case NbtType.tagEnd:
				throw new NbtParseException.UnexpectedTagType(null, NbtType.tagEnd);
			case NbtType.tagByte: {
				if (parentSchema.deniesByteList(index, len)) throw new NbtParseException.IncorrectSchema(parentSchema);
				byte[] list = di.expectByteArray(len);
				if (parentSchema.deniesByteList(index, list)) throw new NbtParseException.IncorrectSchema(parentSchema);
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagShort: {
				if (parentSchema.deniesShortList(index, len)) throw new NbtParseException.IncorrectSchema(parentSchema);
				short[] list = di.expectShortArray(len);
				if (parentSchema.deniesShortList(index, list))
					throw new NbtParseException.IncorrectSchema(parentSchema);
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagInt: {
				if (parentSchema.deniesIntList(index, len)) throw new NbtParseException.IncorrectSchema(parentSchema);
				int[] list = di.expectIntArray(len);
				if (parentSchema.deniesIntList(index, list)) throw new NbtParseException.IncorrectSchema(parentSchema);
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagLong: {
				if (parentSchema.deniesLongList(index, len)) throw new NbtParseException.IncorrectSchema(parentSchema);
				long[] list = di.expectLongArray(len);
				if (parentSchema.deniesLongList(index, list)) throw new NbtParseException.IncorrectSchema(parentSchema);
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagFloat: {
				if (parentSchema.deniesFloatList(index, len)) throw new NbtParseException.IncorrectSchema(parentSchema);
				float[] list = di.expectFloatArray(len);
				if (parentSchema.deniesFloatList(index, list))
					throw new NbtParseException.IncorrectSchema(parentSchema);
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagDouble: {
				if (parentSchema.deniesDoubleList(index, len))
					throw new NbtParseException.IncorrectSchema(parentSchema);
				double[] list = di.expectDoubleArray(len);
				if (parentSchema.deniesDoubleList(index, list))
					throw new NbtParseException.IncorrectSchema(parentSchema);
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagByteArray: {
				NbtListOfByteArraysSchema schema = nonNullSchema(
					parentSchema, parentSchema.schemaForListOfByteArrays(index, len));
				byte[][] list = readGenericArray(len, byte[][]::new, i -> readByteArray(schema, i));
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagIntArray: {
				NbtListOfIntArraysSchema schema = nonNullSchema(
					parentSchema, parentSchema.schemaForListOfIntArrays(index, len));
				int[][] list = readGenericArray(len, int[][]::new, i -> readIntArray(schema, i));
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagLongArray: {
				NbtListOfLongArraysSchema schema = nonNullSchema(
					parentSchema, parentSchema.schemaForListOfLongArrays(index, len));
				long[][] list = readGenericArray(len, long[][]::new, i -> readLongArray(schema, i));
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagString: {
				NbtListOfStringsSchema schema = nonNullSchema(
					parentSchema, parentSchema.schemaForListOfStrings(index, len));
				String[] list = readGenericArray(len, String[]::new, i -> readString(schema, i));
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagList:
				nestedTarget.push(ListOfListsTarget.create(oomAware, null, len, nonNullSchema(
					parentSchema, parentSchema.schemaForListOfLists(index, len))));
				return false;
			case NbtType.tagCompound:
				readListOfCompoundsValue(nonNullSchema(
											 parentSchema, parentSchema.schemaForListOfCompounds(index, len)),
										 len, handleList);
				return true;
			default:
				throw new NbtParseException.UnknownTagType(type);
		}
		handleList.accept(listToBeHandled);
		return true;
	}

	/**
	 * Reads a NBT List value if it is not nested (List of Lists or List of Maps) and returns {@code true}.
	 * <p>If the parsed NBT List is a List of Lists, it will return {@code false}, and a {@link ListOfListsTarget}
	 * target will be added to the top of the stack.
	 * <p>If the parsed NBT List is a List of Maps, an array of {@link ValuelessNbtCompound} will be created, and
	 * each of those will be added to the top of the stack as {@link CompoundTarget}s, so that they can be parsed.
	 * If the corresponding {@link NbtListOfCompoundsSchema} is an instance of
	 * {@link SaveCompoundSchema.ListOfCompounds}, the mentioned {@link ValuelessNbtCompound}s will be
	 * {@link NbtCompound}s, and a {@link NbtList} of them will be handled when all finish parsing. Otherwise, an
	 * empty list will be handled when all have finished parsing.
	 */
	private boolean readListValue(@NotNull String key, @NotNull NbtCompoundSchema parentSchema,
								  @NotNull CheckedConsumer<@NotNull NbtList, OomException> handleList)
		throws ReadException, EofException, OomException, NbtParseException, FixedStack.FullStackException {
		int type = di.expectByte();
		int len = readArrayLen();
		if (len == 0) {
			if (parentSchema.deniesEmptyList(key)) throw new NbtParseException.IncorrectSchema(parentSchema);
			handleList.accept(NbtList.EMPTY_LIST);
			return true;
		}
		NbtList listToBeHandled;
		switch (type) {
			case NbtType.tagEnd:
				throw new NbtParseException.UnexpectedTagType(null, NbtType.tagEnd);
			case NbtType.tagByte: {
				if (parentSchema.deniesByteList(key, len)) throw new NbtParseException.IncorrectSchema(parentSchema);
				byte[] list = di.expectByteArray(len);
				if (parentSchema.deniesByteList(key, list)) throw new NbtParseException.IncorrectSchema(parentSchema);
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagShort: {
				if (parentSchema.deniesShortList(key, len)) throw new NbtParseException.IncorrectSchema(parentSchema);
				short[] list = di.expectShortArray(len);
				if (parentSchema.deniesShortList(key, list)) throw new NbtParseException.IncorrectSchema(parentSchema);
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagInt: {
				if (parentSchema.deniesIntList(key, len)) throw new NbtParseException.IncorrectSchema(parentSchema);
				int[] list = di.expectIntArray(len);
				if (parentSchema.deniesIntList(key, list)) throw new NbtParseException.IncorrectSchema(parentSchema);
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagLong: {
				if (parentSchema.deniesLongList(key, len)) throw new NbtParseException.IncorrectSchema(parentSchema);
				long[] list = di.expectLongArray(len);
				if (parentSchema.deniesLongList(key, list)) throw new NbtParseException.IncorrectSchema(parentSchema);
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagFloat: {
				if (parentSchema.deniesFloatList(key, len)) throw new NbtParseException.IncorrectSchema(parentSchema);
				float[] list = di.expectFloatArray(len);
				if (parentSchema.deniesFloatList(key, list)) throw new NbtParseException.IncorrectSchema(parentSchema);
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagDouble: {
				if (parentSchema.deniesDoubleList(key, len)) throw new NbtParseException.IncorrectSchema(parentSchema);
				double[] list = di.expectDoubleArray(len);
				if (parentSchema.deniesDoubleList(key, list)) throw new NbtParseException.IncorrectSchema(parentSchema);
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagByteArray: {
				NbtListOfByteArraysSchema schema = nonNullSchema(
					parentSchema, parentSchema.schemaForListOfByteArrays(key, len));
				byte[][] list = readGenericArray(len, byte[][]::new, i -> readByteArray(schema, i));
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagIntArray: {
				NbtListOfIntArraysSchema schema = nonNullSchema(
					parentSchema, parentSchema.schemaForListOfIntArrays(key, len));
				int[][] list = readGenericArray(len, int[][]::new, i -> readIntArray(schema, i));
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagLongArray: {
				NbtListOfLongArraysSchema schema = nonNullSchema(
					parentSchema, parentSchema.schemaForListOfLongArrays(key, len));
				long[][] list = readGenericArray(len, long[][]::new, i -> readLongArray(schema, i));
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagString: {
				NbtListOfStringsSchema schema = nonNullSchema(
					parentSchema, parentSchema.schemaForListOfStrings(key, len));
				String[] list = readGenericArray(len, String[]::new, i -> readString(schema, i));
				listToBeHandled = NbtList.emptyUnsafeCreate(oomAware, list);
				break;
			}
			case NbtType.tagList:
				nestedTarget.push(ListOfListsTarget.create(oomAware, key, len, nonNullSchema(
					parentSchema, parentSchema.schemaForListOfLists(key, len))));
				return false;
			case NbtType.tagCompound:
				readListOfCompoundsValue(nonNullSchema(
											 parentSchema, parentSchema.schemaForListOfCompounds(key, len)),
										 len, handleList);
				return true;
			default:
				throw new NbtParseException.UnknownTagType(type);
		}
		handleList.accept(listToBeHandled);
		return true;
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
		if (schema.deniesString(key, stringLen, false)) throw new NbtParseException.IncorrectSchema(schema);
		String value;
		try {
			value = di.expectModifiedUtf8((short) stringLen);
		} catch (CheckedBigEndianDataInput.ModifiedUtf8DataFormatException ex) {
			throw new NbtParseException.InvalidString(ex);
		}
		if (schema.deniesString(key, value)) throw new NbtParseException.IncorrectSchema(schema);
		return value;
	}

	@NotNull
	private String readString(@NotNull NbtListOfStringsSchema schema, int index)
		throws ReadException, EofException, OomException, NbtParseException {
		int stringLen = di.expectShort();
		if (schema.deniesString(index, stringLen, false)) throw new NbtParseException.IncorrectSchema(schema);
		String value;
		try {
			value = di.expectModifiedUtf8((short) stringLen);
		} catch (CheckedBigEndianDataInput.ModifiedUtf8DataFormatException ex) {
			throw new NbtParseException.InvalidString(ex);
		}
		if (schema.deniesString(index, value)) throw new NbtParseException.IncorrectSchema(schema);
		return value;
	}

	private int readArrayLen() throws ReadException, EofException, NbtParseException.InvalidDataStructureSize {
		int len = di.expectInt();
		if (len < 0 | len > GrowableArray.MAX_ARRAY_SIZE) throw new NbtParseException.InvalidDataStructureSize(len);
		return len;
	}

	private byte @NotNull [] readByteArray(@NotNull NbtCompoundSchema schema, @NotNull String key)
		throws ReadException, EofException, OomException, NbtParseException {
		int arrayLen = readArrayLen();
		if (schema.deniesByteArray(key, arrayLen)) throw new NbtParseException.IncorrectSchema(schema);
		byte[] value = di.expectByteArray(arrayLen);
		if (schema.deniesByteArray(key, value)) throw new NbtParseException.IncorrectSchema(schema);
		return value;
	}

	private byte @NotNull [] readByteArray(@NotNull NbtListOfByteArraysSchema schema, int index)
		throws ReadException, EofException, OomException, NbtParseException {
		int arrayLen = readArrayLen();
		if (schema.deniesByteArray(index, arrayLen)) throw new NbtParseException.IncorrectSchema(schema);
		byte[] value = di.expectByteArray(arrayLen);
		if (schema.deniesByteArray(index, value)) throw new NbtParseException.IncorrectSchema(schema);
		return value;
	}

	private int @NotNull [] readIntArray(@NotNull NbtCompoundSchema schema, @NotNull String key)
		throws ReadException, EofException, OomException, NbtParseException {
		int arrayLen = readArrayLen();
		if (schema.deniesIntArray(key, arrayLen)) throw new NbtParseException.IncorrectSchema(schema);
		int[] value = di.expectIntArray(arrayLen);
		if (schema.deniesIntArray(key, value)) throw new NbtParseException.IncorrectSchema(schema);
		return value;
	}

	private int @NotNull [] readIntArray(@NotNull NbtListOfIntArraysSchema schema, int index)
		throws ReadException, EofException, OomException, NbtParseException {
		int arrayLen = readArrayLen();
		if (schema.deniesIntArray(index, arrayLen)) throw new NbtParseException.IncorrectSchema(schema);
		int[] value = di.expectIntArray(arrayLen);
		if (schema.deniesIntArray(index, value)) throw new NbtParseException.IncorrectSchema(schema);
		return value;
	}

	private long @NotNull [] readLongArray(@NotNull NbtCompoundSchema schema, @NotNull String key)
		throws ReadException, EofException, OomException, NbtParseException {
		int arrayLen = readArrayLen();
		if (schema.deniesLongArray(key, arrayLen)) throw new NbtParseException.IncorrectSchema(schema);
		long[] value = di.expectLongArray(arrayLen);
		if (schema.deniesLongArray(key, value)) throw new NbtParseException.IncorrectSchema(schema);
		return value;
	}

	private long @NotNull [] readLongArray(@NotNull NbtListOfLongArraysSchema schema, int index)
		throws ReadException, EofException, OomException, NbtParseException {
		int arrayLen = readArrayLen();
		if (schema.deniesLongArray(index, arrayLen)) throw new NbtParseException.IncorrectSchema(schema);
		long[] value = di.expectLongArray(arrayLen);
		if (schema.deniesLongArray(index, value)) throw new NbtParseException.IncorrectSchema(schema);
		return value;
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

	private static class CompoundTarget implements ParsingTarget {
		private @NotNull NbtCompoundSchema schema;
		private @NotNull ValuelessNbtCompound compound;

		private CompoundTarget(@NotNull ValuelessNbtCompound compound, @NotNull NbtCompoundSchema schema) {
			this.schema = schema;
			this.compound = compound;
		}

		public boolean advanceIsFinished() throws OomException, NbtParseException.IncorrectSchema {
			return true;
		}
	}

	private static final class ListOfCompoundsTarget extends CompoundTarget {
		private final @NotNull NbtListOfCompoundsSchema parentSchema;
		private final @NotNull ValuelessNbtCompound @NotNull [] compounds;
		private final @NotNull CheckedRunnable<OomException> onFinish;
		private int i = 0;

		@SuppressWarnings("DataFlowIssue")
		private ListOfCompoundsTarget(
			@NotNull ValuelessNbtCompound @NotNull [] compounds, @NotNull NbtListOfCompoundsSchema parentSchema,
			@NotNull CheckedRunnable<OomException> onFinish) {
			super(null, null);
			this.parentSchema = parentSchema;
			this.compounds = compounds;
			this.onFinish = onFinish;
		}

		@Override
		public boolean advanceIsFinished() throws OomException, NbtParseException.IncorrectSchema {
			int i = this.i++;
			ValuelessNbtCompound[] compounds = this.compounds;
			if (i == compounds.length) {
				onFinish.run();
				return true;
			}
			assert i < compounds.length;
			super.schema = nonNullSchema(parentSchema, parentSchema.schemaForCompound(i));
			super.compound = compounds[i];
			return false;
		}
	}

	private static final class ListOfListsTarget implements ParsingTarget {
		private final @NotNull NbtListOfListsSchema schema;

		private final @NotNull NbtList result;
		private final @Nullable String key;

		private final @NotNull NbtList @NotNull [] array;
		private int savedIndex;

		private static ListOfListsTarget create(@Nullable OomAware oomAware, @Nullable String key, int len,
												@NotNull NbtListOfListsSchema schema) throws OomException {
			return OomAware.tryRun(oomAware, () -> new ListOfListsTarget(oomAware, key, len, schema));
		}

		private ListOfListsTarget(@Nullable OomAware oomAware, @Nullable String key, int len,
								  @NotNull NbtListOfListsSchema schema) throws OomException {
			assert len > 0;

			this.schema = schema;
			this.key = key;
			this.array = new NbtList[len];
			this.result = NbtList.emptyUnsafeCreate(oomAware, array);
		}
	}
}
