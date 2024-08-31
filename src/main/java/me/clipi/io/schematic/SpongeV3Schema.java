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

package me.clipi.io.schematic;

import me.clipi.io.OomException;
import me.clipi.io.OomException.OomAware;
import me.clipi.io.generic_mc.parse.NbtBlockEntity;
import me.clipi.io.generic_mc.parse.NbtEntity;
import me.clipi.io.nbt.NbtCompound;
import me.clipi.io.nbt.SaveCompoundSchema;
import me.clipi.io.nbt.schema.DelegatedCompoundSchema;
import me.clipi.io.nbt.schema.DenyAllCompoundSchema;
import me.clipi.io.nbt.schema.NbtCompoundSchema;
import me.clipi.io.nbt.schema.NbtListOfCompoundsSchema;
import me.clipi.io.nbt.schema.NbtListOfCompoundsSchema.SchemaList;
import me.clipi.io.util.GrowableArray;
import me.clipi.io.util.VarIntLong;
import me.clipi.io.util.function.CheckedSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.IntFunction;

public class SpongeV3Schema<ResourceType, BlockStateType, BlockType, BiomeType, EntityType> extends DenyAllCompoundSchema {
	private final @NotNull OomAware oomAware;
	private final @NotNull Class<BlockStateType> blockStateClass;
	private final @NotNull Class<BlockType> blockClass;
	private final @NotNull Class<BiomeType> biomeClass;
	private final @NotNull Class<EntityType> entityClass;
	private final @NotNull Function<@NotNull String, @Nullable ResourceType> tryParseResource;
	private final @NotNull IntFunction<
		@Nullable DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType>> tryDataVersionInfo;

	/**
	 * package-private
	 */
	SpongeV3Schema(@NotNull OomAware oomAware,
				   @NotNull Class<BlockStateType> blockStateClass,
				   @NotNull Class<BlockType> blockClass,
				   @NotNull Class<BiomeType> biomeClass,
				   @NotNull Class<EntityType> entityClass,
				   @NotNull Function<@NotNull String, @Nullable ResourceType> tryParseResource,
				   @NotNull IntFunction<
					   @Nullable DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType>> tryDataVersionInfo) {
		this.oomAware = oomAware;

		this.blockStateClass = blockStateClass;
		this.blockClass = blockClass;
		this.biomeClass = biomeClass;
		this.entityClass = entityClass;

		this.tryParseResource = tryParseResource;
		this.tryDataVersionInfo = new IntFunction<
			DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType>>() {
			private @Nullable DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType> cached;
			private boolean isComputed;

			@Override
			public DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType> apply(int dataVersion) {
				if (isComputed) return cached;
				isComputed = true;
				return cached = tryDataVersionInfo.apply(dataVersion);
			}
		};
	}

	private boolean hasVersion, hasXLen, hasYLen, hasZLen;
	private int xOff, yOff, zOff;

	private int dataVersion;
	private @Range(from = 0, to = (1 << 16) - 1) int xLen, yLen, zLen;
	private @Nullable DelegatedCompoundSchema<BlocksSchema> blocks;
	private @Nullable DelegatedCompoundSchema<BiomesSchema> biomes;
	private @Nullable SchemaList<EntitySchema<ResourceType>> entities;

	Schematic<BlockType, BiomeType, EntityType> schematic;

	@Override
	public void toString(@NotNull Nester nester) {
		// TODO
	}

	@Nullable
	private DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType> getDataVersionInfoWithDimensions() {
		return hasXLen & hasYLen & hasZLen & dataVersion > 0 ? tryDataVersionInfo.apply(dataVersion) : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean deniesFinishedCompound() throws OomException {
		DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType> dataVersionInfo;
		if (!(hasVersion & (dataVersionInfo = getDataVersionInfoWithDimensions()) != null)) return true;
		EntityType[] entities;
		if (this.entities == null) {
			entities = null;
		} else {
			EntitySchema<ResourceType>[] schemas = this.entities.schemas;
			int i = schemas.length;
			entities = (EntityType[]) Array.newInstance(entityClass, i);
			while (--i >= 0) {
				int finalI = i;
				entities[i] = oomAware.tryRun(() -> dataVersionInfo.tryParseEntity.apply(schemas[finalI].into()));
			}
		}
		@NotNull BlockType[] @NotNull [] @NotNull [] blocks;
		if (this.blocks == null) {
			blocks = null;
		} else if (this.blocks.computeSchema() == null) {
			return true;
		} else {
			blocks = this.blocks.computeSchema().yzxElement;
		}
		@NotNull BiomeType[] @NotNull [] @NotNull [] biomes;
		if (this.biomes == null) {
			biomes = null;
		} else if (this.biomes.computeSchema() == null) {
			return true;
		} else {
			biomes = this.biomes.computeSchema().yzxElement;
		}
		schematic = oomAware.tryRun(() -> new Schematic<>(dataVersion, xOff, yOff, zOff, xLen, yLen, zLen,
														  blocks, biomes, entities));
		return false;
	}

	@Override
	public boolean deniesInt(@NotNull String key, int value) {
		switch (key) {
			case "Version":
				hasVersion = true;
				return value != 3;
			case "DataVersion":
				dataVersion = value;
				return value <= 0;
			default:
				return true;
		}
	}

	@Override
	public boolean deniesShort(@NotNull String key, @Range(from = 0, to = (1 << 16) - 1) int value) {
		switch (key) {
			case "Width":
				xLen = value;
				hasXLen = true;
				return false;
			case "Height":
				yLen = value;
				hasYLen = true;
				return false;
			case "Length":
				zLen = value;
				hasZLen = true;
				return false;
			default:
				return true;
		}
	}

	@Override
	public boolean deniesIntArray(@NotNull String key, int length) {
		return !("Offset".equals(key) & length == 3);
	}

	@Override
	public boolean deniesIntArray(@NotNull String key, int @NotNull [] value) {
		xOff = value[0];
		yOff = value[1];
		zOff = value[2];
		return false;
	}

	@Override
	public @Nullable NbtCompoundSchema schemaForCompound(@NotNull String key) throws OomException {
		switch (key) {
			case "Metadata":
				return NbtCompoundSchema.ALWAYS;
			case "Blocks":
				return (blocks =
					DelegatedCompoundSchema.create(oomAware, () -> {
						DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType> dataVersionInfo =
							getDataVersionInfoWithDimensions();
						return dataVersionInfo == null ? null :
							new BlocksSchema(dataVersionInfo, xLen, yLen, zLen);
					})
				).rawSchema();
			case "Biomes":
				return (biomes = DelegatedCompoundSchema.create(oomAware, () -> {
					DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType> dataVersionInfo =
						getDataVersionInfoWithDimensions();
					return dataVersionInfo == null ? null :
						new BiomesSchema(dataVersionInfo.tryParseBiome, xLen, yLen, zLen);
				})).rawSchema();
			default:
				return null;
		}
	}

	@Override
	public boolean deniesEmptyList(@NotNull String key) {
		return !"Entities".equals(key);
	}

	@Override
	public @Nullable NbtListOfCompoundsSchema schemaForListOfCompounds(
		@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
		return "Entities".equals(key) ?
			entities = schemaList(oomAware, length, EntitySchema.class, () ->
				new EntitySchema<>(oomAware, tryParseResource)) :
			null;
	}

	private static <T extends NbtCompoundSchema, R extends NbtCompoundSchema> SchemaList<R> schemaList(
		@NotNull OomAware oomAware, int length, @NotNull Class<T> tClass,
		@NotNull CheckedSupplier<T, OomException> generateSchema) throws OomException {
		SchemaList<?> raw = SchemaList.create(oomAware, length, tClass, generateSchema);
		@SuppressWarnings("unchecked")
		SchemaList<R> res = (SchemaList<R>) raw;
		return res;
	}

	private static class EntitySchema<ResourceType> extends IdAndDataSchema<ResourceType> {
		private double x, y, z;

		public EntitySchema(
			@NotNull OomAware oomAware, @NotNull Function<@NotNull String, @Nullable ResourceType> tryParse) {
			super(oomAware, tryParse);
		}

		@Override
		public void toString(@NotNull Nester nester) {
			// TODO
		}

		@NotNull
		private NbtEntity<ResourceType> into() throws OomException {
			return super.oomAware.tryRun(() -> new NbtEntity<>(super.id, super.data, x, y, z));
		}

		@Override
		public boolean deniesDoubleList(
			@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
			return !("Pos".equals(key) & length == 3);
		}

		@Override
		public boolean deniesDoubleList(@NotNull String key, double @NotNull [] value) {
			x = value[0];
			y = value[1];
			z = value[2];
			return false;
		}
	}

	private static class BlockEntitySchema<ResourceType> extends IdAndDataSchema<ResourceType> {
		private final int xLen, xzLen, zLen, yLen;

		private int x, y, z, pos = -1;

		public BlockEntitySchema(@NotNull OomAware oomAware,
								 @NotNull Function<@NotNull String, @Nullable ResourceType> tryParse,
								 int xLen, int zLen, int yLen) {
			super(oomAware, tryParse);
			this.xLen = xLen;
			this.xzLen = xLen * zLen;
			this.zLen = zLen;
			this.yLen = yLen;
		}

		@Override
		public void toString(@NotNull Nester nester) {
			// TODO
		}

		@NotNull
		private <BlockStateType> NbtBlockEntity<ResourceType, BlockStateType> into(@NotNull BlockStateType blockState) throws OomException {
			return super.oomAware.tryRun(() -> new NbtBlockEntity<>(super.id, blockState, super.data, x, y, z));
		}

		@Override
		public boolean deniesFinishedCompound() {
			return pos < 0 | super.deniesFinishedCompound();
		}

		@Override
		public boolean deniesIntArray(
			@NotNull String key, @Range(from = 0, to = GrowableArray.MAX_ARRAY_SIZE) int length) {
			return !("Pos".equals(key) & length == 3);
		}

		@Override
		public boolean deniesIntArray(@NotNull String key, int @NotNull [] value) {
			int x = value[0], y = value[1], z = value[2];
			this.x = x;
			this.y = y;
			this.z = z;
			this.pos = x + z * xLen + y * xzLen;
			return (x | y | z) < 0 | x >= xLen | y >= yLen | z >= zLen;
		}
	}

	private abstract static class IdAndDataSchema<ResourceType> extends DenyAllCompoundSchema {
		private final @NotNull OomAware oomAware;
		private final @NotNull Function<@NotNull String, @Nullable ResourceType> tryParse;

		private ResourceType id;
		private @Nullable NbtCompound data;

		public IdAndDataSchema(@NotNull OomAware oomAware,
							   @NotNull Function<@NotNull String, @Nullable ResourceType> tryParse) {
			this.oomAware = oomAware;
			this.tryParse = tryParse;
		}

		@Override
		public boolean deniesFinishedCompound() {
			return this.id == null;
		}

		@Override
		public boolean deniesString(
			@NotNull String key, @Range(from = 0, to = (1 << 16) - 1) int modifiedUtf8ByteLength) {
			return !"Id".equals(key);
		}

		@Override
		public boolean deniesString(@NotNull String key, @NotNull String value) throws OomException {
			return null == (this.id = oomAware.tryRun(() -> tryParse.apply(value)));
		}

		@Override
		public @Nullable NbtCompoundSchema schemaForCompound(@NotNull String key) throws OomException {
			if ("Data".equals(key)) {
				SaveCompoundSchema schema = SaveCompoundSchema.create(oomAware);
				data = schema.compound;
				return schema;
			}
			return null;
		}
	}

	private class BiomesSchema extends PaletteAndDataSchema<BiomeType, BiomeType> {
		private BiomesSchema(
			@NotNull Function<@NotNull ResourceType, @Nullable BiomeType> tryParseBiome,
			@Range(from = 0, to = (1 << 16) - 1) int xLen,
			@Range(from = 0, to = (1 << 16) - 1) int yLen,
			@Range(from = 0, to = (1 << 16) - 1) int zLen) throws OomException {
			super(oomAware, biomeClass, biomeClass, id -> {
				ResourceType res = tryParseResource.apply(id);
				return res == null ? null : tryParseBiome.apply(res);
			}, xLen, yLen, zLen);
		}

		@Override
		public void toString(@NotNull Nester nester) {
			// TODO
		}

		@Override
		@NotNull
		protected BiomeType transform(@NotNull BiomeType biome, int x, int y, int z, int pos) {
			return biome;
		}
	}

	private class BlocksSchema extends PaletteAndDataSchema<BlockStateType, BlockType> {
		private final @NotNull DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType> dataVersionInfo;
		private BlockEntitySchema<ResourceType> @Nullable [] blockEntities;

		private BlocksSchema(
			@NotNull DataVersionInfo<ResourceType, BlockStateType, BlockType, BiomeType, EntityType> dataVersionInfo,
			@Range(from = 0, to = (1 << 16) - 1) int xLen,
			@Range(from = 0, to = (1 << 16) - 1) int yLen,
			@Range(from = 0, to = (1 << 16) - 1) int zLen) throws OomException {
			super(oomAware, blockStateClass, blockClass, id -> {
				int len = id.length(), lenM1 = len - 1;
				int i = id.indexOf('[');
				int resourceUntil = i;
				if (i >= 0 && (id.indexOf('[', ++i) >= 0 || id.indexOf(']') != lenM1))
					return null;
				
				ResourceType resource = tryParseResource.apply(
					resourceUntil < 0 ? id : id.substring(0, resourceUntil));
				if (resource == null) return null;
				DataVersionInfo.BlockStateBuilder<BlockStateType> builder =
					dataVersionInfo.tryParseBlockState.apply(resource);
				if (builder == null) return null;
				if (resourceUntil < 0) return builder.build();

				HashSet<String> dejaVu = new HashSet<>();
				int lastComma = id.lastIndexOf(',', lenM1);
				for (; i < lastComma; ++i) {
					int comma = id.indexOf(',', i), eq = id.indexOf('=', i);
					if (eq >= 0 & (comma < 0 | comma > eq)) {
						String key = id.substring(i, eq), value = id.substring(++eq, i = comma);
						if (id.indexOf('=', eq) >= comma &&
							dejaVu.add(key) &&
							builder.addProperty(key, value)) continue;
					}
					return null;
				}
				int lastEq = id.indexOf('=', i);
				if (lastEq >= 0) {
					String key = id.substring(i, lastEq), value = id.substring(++lastEq, lenM1);
					if (id.indexOf('=', lastEq) < 0 &&
						dejaVu.add(key) &&
						builder.addProperty(key, value)) return builder.build();
				}
				return null;
			}, xLen, yLen, zLen);
			this.dataVersionInfo = dataVersionInfo;
		}

		@Override
		public void toString(@NotNull Nester nester) {
			// TODO
		}

		@Override
		public boolean deniesFinishedCompound() throws OomException {
			if (blockEntities != null)
				Arrays.sort(blockEntities, Comparator.comparingInt(be -> be.pos));
			return super.deniesFinishedCompound();
		}

		@Override
		@Nullable
		protected BlockType transform(@NotNull BlockStateType blockState, int x, int y, int z, int pos) throws OomException {
			if (blockEntities != null) {
				int idx = Arrays.binarySearch(blockEntities, pos, Comparator.comparingInt(
					o -> o instanceof Integer ? (int) o : ((BlockEntitySchema<?>) o).pos));
				if (idx >= 0) return dataVersionInfo.tryNbtBlock.apply(x, y, z, blockEntities[idx].into(blockState));
			}
			return dataVersionInfo.tryDefaultBlock.apply(x, y, z, blockState);
		}

		@Override
		public boolean deniesEmptyList(@NotNull String key) {
			return !"BlockEntities".equals(key);
		}

		@Override
		public @Nullable NbtListOfCompoundsSchema schemaForListOfCompounds(
			@NotNull String key, @Range(from = 1, to = GrowableArray.MAX_ARRAY_SIZE) int length) throws OomException {
			if ("BlockEntities".equals(key)) {
				SchemaList<BlockEntitySchema<ResourceType>> listSchema = schemaList(
					oomAware, length, BlockEntitySchema.class, () ->
						new BlockEntitySchema<>(oomAware, tryParseResource, super.xLen, super.zLen, super.yLen));
				blockEntities = listSchema.schemas;
				return listSchema;
			}
			return null;
		}
	}

	private abstract static class PaletteAndDataSchema<T, R> extends DenyAllCompoundSchema {
		private final @NotNull OomAware oomAware;
		private final @NotNull Class<T> tClass;
		private final @NotNull Function<@NotNull String, @Nullable T> tryParse;
		private final @Range(from = 0, to = (1 << 16) - 1) int xLen, yLen, zLen;
		private final @Range(from = 0, to = GrowableArray.MAX_ARRAY_SIZE) int xzLen, xyzLen;

		private PaletteSchema<T> palette;
		private byte[] data;
		protected final @NotNull R @NotNull [] @NotNull [] @NotNull [] yzxElement;

		@SuppressWarnings("unchecked")
		private PaletteAndDataSchema(@NotNull OomAware oomAware,
									 @NotNull Class<T> tClass,
									 @NotNull Class<R> rClass,
									 @NotNull Function<@NotNull String, @Nullable T> tryParse,
									 @Range(from = 0, to = (1 << 16) - 1) int xLen,
									 @Range(from = 0, to = (1 << 16) - 1) int yLen,
									 @Range(from = 0, to = (1 << 16) - 1) int zLen) throws OomException {
			this.oomAware = oomAware;
			this.tClass = tClass;
			this.tryParse = tryParse;
			this.xLen = xLen;
			this.yLen = yLen;
			this.zLen = zLen;
			long xzLen = (long) xLen * zLen;
			long xyzLen = xzLen * yLen;
			if (xyzLen > GrowableArray.MAX_ARRAY_SIZE) throw OomException.INSTANCE;
			this.xzLen = (int) xzLen;
			this.xyzLen = (int) (xzLen * yLen);
			yzxElement = oomAware.tryRun(() -> (R[][][]) Array.newInstance(rClass, yLen, zLen, xLen));
		}

		@Override
		public boolean deniesFinishedCompound() throws OomException {
			if (palette == null | data == null) return true;
			final boolean[] hasErrors = { false, false };
			try {
				VarIntLong.parseVarInts(data, (idx, pos) -> {
					int x = pos % xLen;
					int z = (pos / xLen) % zLen;
					int y = pos / xzLen;
					T t = idx >= 0 ? palette.positiveArray.inner[idx] : palette.negArrayShiftedOne.inner[-++idx];
					R r;
					try {
						r = t == null ? null : transform(t, x, y, z, pos);
					} catch (OomException e) {
						hasErrors[1] = true;
						return false;
					}
					if (r == null) {
						hasErrors[0] = true;
						return false;
					}
					yzxElement[y][z][x] = r;
					return true;
				});
			} catch (VarIntLong.ParseVarIntLongException ex) {
				return true;
			}
			if (hasErrors[1]) throw OomException.INSTANCE;
			return hasErrors[0];
		}

		@Nullable
		protected abstract R transform(@NotNull T t, int x, int y, int z, int pos) throws OomException;

		@Override
		public boolean deniesByteArray(@NotNull String key, int length) {
			return !("Data".equals(key) & length == xyzLen);
		}

		@Override
		public boolean deniesByteArray(@NotNull String key, byte @NotNull [] value) {
			data = value;
			return false;
		}

		@Override
		public @Nullable NbtCompoundSchema schemaForCompound(@NotNull String key) throws OomException {
			return "Palette".equals(key) ? palette = new PaletteSchema<>(oomAware, tClass, xyzLen, tryParse) : null;
		}
	}

	private static class PaletteSchema<T> extends DenyAllCompoundSchema {
		private final @NotNull OomAware oomAware;
		private final @NotNull Function<@NotNull String, @Nullable T> tryParse;

		private final @NotNull GrowableArray<T[]> positiveArray, negArrayShiftedOne;

		private final int maxElements;
		private int currentElementCount;

		private PaletteSchema(@NotNull OomAware oomAware, @NotNull Class<T> tClass, int maxElements,
							  @NotNull Function<@NotNull String, @Nullable T> tryParse) throws OomException {
			this.oomAware = oomAware;
			this.maxElements = maxElements;
			this.tryParse = tryParse;
			// TODO We have maxElements, which is a pretty good estimate of the biggest index
			positiveArray = GrowableArray.generic(tClass, this.oomAware);
			negArrayShiftedOne = GrowableArray.generic(tClass, this.oomAware);
		}

		@Override
		public void toString(@NotNull Nester nester) {
			// TODO
		}

		@Override
		public boolean deniesFinishedCompound() {
			return false;
		}

		@Override
		public boolean deniesInt(@NotNull String key, int idx) throws OomException {
			if (++currentElementCount > maxElements) return true;
			GrowableArray<T[]> tempArray;
			if (idx >= 0) {
				tempArray = positiveArray;
			} else {
				idx = -++idx;
				tempArray = negArrayShiftedOne;
			}
			if (idx >= tempArray.getSize())
				tempArray.zeroExtend(idx);
			T[] asArray = tempArray.inner;
			if (asArray[idx] != null) return true; // Duplicated key
			T parsed = oomAware.tryRunOrNull(() -> {
				// noinspection DataFlowIssue
				return tryParse.apply(key);
			});
			asArray[idx] = parsed;
			return parsed == null;
		}
	}
}
