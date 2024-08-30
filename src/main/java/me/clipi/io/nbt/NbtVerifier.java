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

import me.clipi.io.OomException;
import me.clipi.io.OomException.OomAware;
import me.clipi.io.nbt.exceptions.NbtKeyNotFoundException;
import me.clipi.io.nbt.exceptions.NbtParseException;
import me.clipi.io.nbt.schema.*;
import me.clipi.io.util.FixedStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NbtVerifier {
	@NotNull
	private static <T> T nonNullSchema(@Nullable T schema) throws NbtParseException.IncorrectSchema {
		if (schema == null) throw new NbtParseException.IncorrectSchema();
		return schema;
	}

	public static boolean isDeniedBySchema(
		@NotNull OomAware oomAware, @NotNull NbtCompound compound, @NotNull NbtCompoundSchema schema) throws OomException {
		if (schema instanceof SaveCompoundSchema) {
			try {
				compound.copyTo(((SaveCompoundSchema) schema).compound);
			} catch (NbtParseException.DuplicatedKey ex) {
				// TODO NbtCompound.copyTo() failed
				return true;
			}
		}

		FixedStack<VerifyingTarget> nestedTarget = oomAware.tryRun(
			() -> new FixedStack<>(VerifyingTarget.class, NbtParser.NESTED_MAX_DEPTH));
		CompoundTarget target = oomAware.tryRun(() -> new CompoundTarget(compound, schema));
		try {
			nestedTarget.push(target);
		} catch (FixedStack.FullStackException ex) {
			throw new IllegalStateException(ex);
		}
		try {
			for (; ; ) {
				ListOfListsTarget nextTarget = verifyMapEntries(oomAware, nestedTarget, target);
				if (nextTarget == null) return false;
				target = verifyListEntries(oomAware, nestedTarget, nextTarget);
			}
		} catch (NbtParseException.IncorrectSchema ex) {
			return true;
		} catch (NbtParseException.DuplicatedKey ex) {
			// TODO NbtCompound.copyTo() failed
			return true;
		}
	}

	/**
	 * <p>Code similar to {@link NbtParser#readMapEntries(NbtParser.CompoundTarget)}
	 */
	@SuppressWarnings("DataFlowIssue")
	private static ListOfListsTarget verifyMapEntries(
		@NotNull OomAware oomAware, @NotNull FixedStack<VerifyingTarget> nestedTarget,
		@NotNull CompoundTarget targetAndSchema) throws OomException, NbtParseException.IncorrectSchema,
														NbtParseException.DuplicatedKey {
		assert nestedTarget.tryPeek() == targetAndSchema;
		newTarget:
		for (; ; ) {
			NbtCompoundSchema schema = targetAndSchema.schema;
			NbtCompound target = targetAndSchema.compound;
			int i = targetAndSchema.savedIndex, mapEntries = target.entries();
			if (i < mapEntries) {
				String[] keys = target.keys.inner;
				byte[] types = target.types.inner;

				byte[] bytes = target.bytes == null ? null : target.bytes.inner;
				short[] shorts = target.shorts == null ? null : target.shorts.inner;
				int[] ints = target.ints == null ? null : target.ints.inner;
				long[] longs = target.longs == null ? null : target.longs.inner;
				float[] floats = target.floats == null ? null : target.floats.inner;
				double[] doubles = target.doubles == null ? null : target.doubles.inner;
				Object[] objects = target.objects == null ? null : target.objects.inner;

				int bCount = targetAndSchema.bCount, sCount = targetAndSchema.sCount, iCount = targetAndSchema.iCount,
					lCount = targetAndSchema.lCount, fCount = targetAndSchema.fCount, dCount = targetAndSchema.dCount,
					oCount = targetAndSchema.oCount;
				do {
					String key = keys[i];
					switch (types[i++]) {
						case NbtType.tagByte:
							if (schema.deniesByte(key, bytes[bCount++]))
								throw new NbtParseException.IncorrectSchema();
							break;
						case NbtType.tagShort:
							if (schema.deniesShort(key, shorts[sCount++]))
								throw new NbtParseException.IncorrectSchema();
							break;
						case NbtType.tagInt:
							if (schema.deniesInt(key, ints[iCount++]))
								throw new NbtParseException.IncorrectSchema();
							break;
						case NbtType.tagLong:
							if (schema.deniesLong(key, longs[lCount++]))
								throw new NbtParseException.IncorrectSchema();
							break;
						case NbtType.tagFloat:
							if (schema.deniesFloat(key, floats[fCount++]))
								throw new NbtParseException.IncorrectSchema();
							break;
						case NbtType.tagDouble:
							if (schema.deniesDouble(key, doubles[dCount++]))
								throw new NbtParseException.IncorrectSchema();
							break;
						case NbtType.tagByteArray: {
							byte[] obj = (byte[]) objects[oCount++];
							if (schema.deniesByteArray(key, obj.length) || schema.deniesByteArray(key, obj))
								throw new NbtParseException.IncorrectSchema();
							break;
						}
						case NbtType.tagIntArray: {
							int[] obj = (int[]) objects[oCount++];
							if (schema.deniesIntArray(key, obj.length) || schema.deniesIntArray(key, obj))
								throw new NbtParseException.IncorrectSchema();
							break;
						}
						case NbtType.tagLongArray: {
							long[] obj = (long[]) objects[oCount++];
							if (schema.deniesLongArray(key, obj.length) || schema.deniesLongArray(key, obj))
								throw new NbtParseException.IncorrectSchema();
							break;
						}
						case NbtType.tagString: {
							String obj = (String) objects[oCount++];
							// TODO this length is not the same as the modifiedUtf8ByteLength!!
							if (schema.deniesString(key, obj.length()) || schema.deniesString(key, obj))
								throw new NbtParseException.IncorrectSchema();
							break;
						}
						case NbtType.tagList: {
							NbtList obj = (NbtList) objects[oCount++];
							Object array = obj.array;
							if (array == null) {
								if (schema.deniesEmptyList(key)) throw new NbtParseException.IncorrectSchema();
								break;
							}
							switch (obj.componentType) {
								case Byte:
									if (schema.deniesByteList(key, ((byte[]) array).length) ||
										schema.deniesByteList(key, (byte[]) array))
										throw new NbtParseException.IncorrectSchema();
									break;
								case Short:
									if (schema.deniesShortList(key, ((short[]) array).length) ||
										schema.deniesShortList(key, (short[]) array))
										throw new NbtParseException.IncorrectSchema();
									break;
								case Int:
									if (schema.deniesIntList(key, ((int[]) array).length) ||
										schema.deniesIntList(key, (int[]) array))
										throw new NbtParseException.IncorrectSchema();
									break;
								case Long:
									if (schema.deniesLongList(key, ((long[]) array).length) ||
										schema.deniesLongList(key, (long[]) array))
										throw new NbtParseException.IncorrectSchema();
									break;
								case Float:
									if (schema.deniesFloatList(key, ((float[]) array).length) ||
										schema.deniesFloatList(key, (float[]) array))
										throw new NbtParseException.IncorrectSchema();
									break;
								case Double:
									if (schema.deniesDoubleList(key, ((double[]) array).length) ||
										schema.deniesDoubleList(key, (double[]) array))
										throw new NbtParseException.IncorrectSchema();
									break;
								case ByteArray:
									if (emptyUnsafeIsDeniedBySchema(
										nonNullSchema(schema.schemaForListOfByteArrays(key,
																					   ((byte[][]) array).length)),
										(byte[][]) array
									)) throw new NbtParseException.IncorrectSchema();
									break;
								case IntArray:
									if (emptyUnsafeIsDeniedBySchema(
										nonNullSchema(schema.schemaForListOfIntArrays(key, ((int[][]) array).length)),
										(int[][]) array
									)) throw new NbtParseException.IncorrectSchema();
									break;
								case LongArray:
									if (emptyUnsafeIsDeniedBySchema(
										nonNullSchema(schema.schemaForListOfLongArrays(key,
																					   ((long[][]) array).length)),
										(long[][]) array
									)) throw new NbtParseException.IncorrectSchema();
									break;
								case String:
									if (emptyUnsafeIsDeniedBySchema(
										nonNullSchema(schema.schemaForListOfStrings(key, ((String[]) array).length)),
										(String[]) array
									)) throw new NbtParseException.IncorrectSchema();
									break;
								case List: {
									NbtList[] listOfLists = (NbtList[]) array;
									ListOfListsTarget res = ListOfListsTarget.create(oomAware, listOfLists,
																					 nonNullSchema(schema.schemaForListOfLists(key, listOfLists.length)));
									try {
										nestedTarget.push(res);
									} catch (FixedStack.FullStackException ex) {
										throw new IllegalStateException(ex);
									}
									targetAndSchema.saveIndices(
										i, bCount, sCount, iCount, lCount, fCount, dCount, oCount);
									return res;
								}
								case Compound:
									targetAndSchema.saveIndices(
										i, bCount, sCount, iCount, lCount, fCount, dCount, oCount);
									targetAndSchema = verifyListOfCompoundsValue(
										oomAware, nestedTarget,
										nonNullSchema(schema.schemaForListOfCompounds(
											key, ((NbtCompound[]) array).length)),
										(NbtCompound[]) array
									);
									continue newTarget;
								case End:
									throw new AssertionError();
							}
							break;
						}
						case NbtType.tagCompound: {
							NbtCompound obj = (NbtCompound) objects[oCount++];
							NbtCompoundSchema newSchema = nonNullSchema(schema.schemaForCompound(key));
							if (newSchema instanceof SaveCompoundSchema)
								obj.copyTo(((SaveCompoundSchema) newSchema).compound);
							targetAndSchema.saveIndices(i, bCount, sCount, iCount, lCount, fCount, dCount, oCount);
							try {
								nestedTarget.push(targetAndSchema = oomAware.tryRun(
									() -> new CompoundTarget(obj, newSchema)));
							} catch (FixedStack.FullStackException ex) {
								throw new IllegalStateException(ex);
							}
							continue newTarget;
						}
						default:
							throw new IllegalStateException();
					}
				} while (i < mapEntries);
			}
			try {
				if (schema.deniesFinishedCompound())
					throw new NbtParseException.IncorrectSchema();
			} catch (NbtParseException.IncorrectSchema ex) {
				throw ex;
			} catch (NbtKeyNotFoundException | NbtParseException cause) {
				throw new NbtParseException.IncorrectSchema(cause);
			}
			if (targetAndSchema.advanceIsFinished()) {
				try {
					nestedTarget.pop(); // pop self
				} catch (FixedStack.EmptyStackException ex) {
					throw new IllegalStateException(ex);
				}

				VerifyingTarget parent = nestedTarget.tryPeek();
				if (parent == null) return null;
				if (parent instanceof ListOfListsTarget) return (ListOfListsTarget) parent;
				targetAndSchema = (CompoundTarget) parent;
			}
		}
	}

	/**
	 * <p>Code similar to {@link NbtParser#readListEntries(NbtParser.ListOfListsTarget)}
	 */
	@NotNull
	private static CompoundTarget verifyListEntries(
		@NotNull OomAware oomAware, @NotNull FixedStack<VerifyingTarget> nestedTarget,
		@NotNull ListOfListsTarget target) throws OomException, NbtParseException.IncorrectSchema,
												  NbtParseException.DuplicatedKey {
		assert nestedTarget.tryPeek() == target;

		newTarget:
		for (; ; ) {
			NbtListOfListsSchema schema = target.schema;
			NbtList[] array = target.listOfLists;
			final int len = array.length;
			int i = target.savedIndex;
			for (; ; ) {
				if (i >= len) {
					VerifyingTarget parent;

					try {
						nestedTarget.pop(); // pop self
						parent = nestedTarget.peek();
					} catch (FixedStack.EmptyStackException ex) {
						throw new IllegalStateException(ex);
					}
					if (parent instanceof CompoundTarget) {
						return (CompoundTarget) parent;
					}

					target = (ListOfListsTarget) parent;
					continue newTarget;
				}


				NbtList obj = array[i];
				Object list = obj.array;
				if (list == null) {
					if (schema.deniesEmptyList(i)) throw new NbtParseException.IncorrectSchema();
					break;
				}
				switch (obj.componentType) {
					case Byte:
						if (schema.deniesByteList(i, ((byte[]) list).length) ||
							schema.deniesByteList(i, (byte[]) list)) throw new NbtParseException.IncorrectSchema();
						break;
					case Short:
						if (schema.deniesShortList(i, ((short[]) list).length) ||
							schema.deniesShortList(i, (short[]) list)) throw new NbtParseException.IncorrectSchema();
						break;
					case Int:
						if (schema.deniesIntList(i, ((int[]) list).length) ||
							schema.deniesIntList(i, (int[]) list)) throw new NbtParseException.IncorrectSchema();
						break;
					case Long:
						if (schema.deniesLongList(i, ((long[]) list).length) ||
							schema.deniesLongList(i, (long[]) list)) throw new NbtParseException.IncorrectSchema();
						break;
					case Float:
						if (schema.deniesFloatList(i, ((float[]) list).length) ||
							schema.deniesFloatList(i, (float[]) list)) throw new NbtParseException.IncorrectSchema();
						break;
					case Double:
						if (schema.deniesDoubleList(i, ((double[]) list).length) ||
							schema.deniesDoubleList(i, (double[]) list)) throw new NbtParseException.IncorrectSchema();
						break;
					case ByteArray:
						if (emptyUnsafeIsDeniedBySchema(
							nonNullSchema(schema.schemaForListOfByteArrays(i, ((byte[][]) list).length)),
							(byte[][]) list
						)) throw new NbtParseException.IncorrectSchema();
						break;
					case IntArray:
						if (emptyUnsafeIsDeniedBySchema(
							nonNullSchema(schema.schemaForListOfIntArrays(i, ((int[][]) list).length)),
							(int[][]) list
						)) throw new NbtParseException.IncorrectSchema();
						break;
					case LongArray:
						if (emptyUnsafeIsDeniedBySchema(
							nonNullSchema(schema.schemaForListOfLongArrays(i, ((long[][]) list).length)),
							(long[][]) list
						)) throw new NbtParseException.IncorrectSchema();
						break;
					case String:
						if (emptyUnsafeIsDeniedBySchema(
							nonNullSchema(schema.schemaForListOfStrings(i, ((String[]) list).length)),
							(String[]) list
						)) throw new NbtParseException.IncorrectSchema();
						break;
					case List: {
						NbtList[] listOfLists = (NbtList[]) list;
						ListOfListsTarget child;
						try {
							nestedTarget.push(child = ListOfListsTarget.create(oomAware, listOfLists,
																			   nonNullSchema(schema.schemaForListOfLists(i, listOfLists.length))));
						} catch (FixedStack.FullStackException ex) {
							throw new IllegalStateException(ex);
						}

						target.savedIndex = ++i;
						target = child;
						continue newTarget;
					}
					case Compound:
						target.savedIndex = ++i;
						return verifyListOfCompoundsValue(
							oomAware, nestedTarget,
							nonNullSchema(schema.schemaForListOfCompounds(
								i, ((NbtCompound[]) list).length)),
							(NbtCompound[]) list
						);
					case End:
						throw new AssertionError();
				}
				++i;
			}
		}
	}

	@NotNull
	private static ListOfCompoundsTarget verifyListOfCompoundsValue(
		@NotNull OomAware oomAware, @NotNull FixedStack<VerifyingTarget> nestedTarget,
		@NotNull NbtListOfCompoundsSchema schema, @NotNull NbtCompound @NotNull [] listOfCompounds) throws OomException, NbtParseException.IncorrectSchema {
		int len = listOfCompounds.length;

		ListOfCompoundsTarget res = oomAware.tryRun(() -> new ListOfCompoundsTarget(listOfCompounds, schema));
		if (res.advanceIsFinished()) throw new AssertionError(
			"Passed len was 0 (sanity check: actual len is = " + len + ")");
		try {
			nestedTarget.push(res);
		} catch (FixedStack.FullStackException ex) {
			throw new IllegalStateException(ex);
		}
		return res;
	}


	// <editor-fold defaultstate="collapsed" desc="isListOfObjectsDeniedBySchema">
	private static boolean emptyUnsafeIsDeniedBySchema(
		@NotNull NbtListOfByteArraysSchema schema, byte @NotNull [] @NotNull [] list) throws OomException {
		for (int i = 0, len = list.length; i < len; ++i) {
			byte[] value = list[i];
			if (schema.deniesByteArray(i, value.length) || schema.deniesByteArray(i, value)) return true;
		}
		return false;
	}

	private static boolean emptyUnsafeIsDeniedBySchema(
		@NotNull NbtListOfIntArraysSchema schema, int @NotNull [] @NotNull [] list) throws OomException {
		for (int i = 0, len = list.length; i < len; ++i) {
			int[] value = list[i];
			if (schema.deniesIntArray(i, value.length) || schema.deniesIntArray(i, value)) return true;
		}
		return false;
	}

	private static boolean emptyUnsafeIsDeniedBySchema(
		@NotNull NbtListOfLongArraysSchema schema, long @NotNull [] @NotNull [] list) throws OomException {
		for (int i = 0, len = list.length; i < len; ++i) {
			long[] value = list[i];
			if (schema.deniesLongArray(i, value.length) || schema.deniesLongArray(i, value)) return true;
		}
		return false;
	}

	private static boolean emptyUnsafeIsDeniedBySchema(
		@NotNull NbtListOfStringsSchema schema, @NotNull String @NotNull [] list) throws OomException {
		for (int i = 0, len = list.length; i < len; ++i) {
			String value = list[i];
			// TODO this length is not the same as the modifiedUtf8ByteLength!!
			if (schema.deniesString(i, value.length()) || schema.deniesString(i, value)) return true;
		}
		return false;
	}
	// </editor-fold>


	private interface VerifyingTarget {
	}

	private static class CompoundTarget implements VerifyingTarget {
		private @NotNull NbtCompoundSchema schema;
		private @NotNull NbtCompound compound;
		private int savedIndex, bCount, sCount, iCount, lCount, fCount, dCount, oCount;

		private CompoundTarget(@NotNull NbtCompound compound, @NotNull NbtCompoundSchema schema) {
			this.schema = schema;
			this.compound = compound;
		}

		public void saveIndices(
			int index, int bCount, int sCount, int iCount, int lCount, int fCount, int dCount, int oCount) {
			this.savedIndex = index;
			this.bCount = bCount;
			this.sCount = sCount;
			this.iCount = iCount;
			this.lCount = lCount;
			this.fCount = fCount;
			this.dCount = dCount;
			this.oCount = oCount;
		}

		public boolean advanceIsFinished() throws OomException, NbtParseException.IncorrectSchema {
			return true;
		}
	}

	private static final class ListOfCompoundsTarget extends CompoundTarget {
		private final @NotNull NbtListOfCompoundsSchema parentSchema;
		private final @NotNull NbtCompound @NotNull [] compounds;
		private int i = 0;

		@SuppressWarnings("DataFlowIssue")
		private ListOfCompoundsTarget(
			@NotNull NbtCompound @NotNull [] compounds, @NotNull NbtListOfCompoundsSchema parentSchema) {
			super(null, null);
			this.parentSchema = parentSchema;
			this.compounds = compounds;
		}

		@Override
		public boolean advanceIsFinished() throws OomException, NbtParseException.IncorrectSchema {
			super.savedIndex = 0;
			super.bCount = 0;
			super.sCount = 0;
			super.iCount = 0;
			super.lCount = 0;
			super.fCount = 0;
			super.dCount = 0;
			super.oCount = 0;
			int i = this.i++;
			NbtCompound[] compounds = this.compounds;
			if (i >= compounds.length) return true;
			super.schema = nonNullSchema(parentSchema.schemaForCompound(i));
			super.compound = compounds[i];
			return false;
		}
	}

	private static final class ListOfListsTarget implements VerifyingTarget {
		private final @NotNull NbtListOfListsSchema schema;

		private final @NotNull NbtList @NotNull [] listOfLists;
		private int savedIndex;

		private static ListOfListsTarget create(
			@NotNull OomAware oomAware, @NotNull NbtList @NotNull [] listOfLists,
			@NotNull NbtListOfListsSchema schema) throws OomException {
			return oomAware.tryRun(() -> new ListOfListsTarget(listOfLists, schema));
		}

		private ListOfListsTarget(@NotNull NbtList @NotNull [] listOfLists, @NotNull NbtListOfListsSchema schema) {
			assert listOfLists.length > 0;

			this.schema = schema;
			this.listOfLists = listOfLists;
		}
	}
}
