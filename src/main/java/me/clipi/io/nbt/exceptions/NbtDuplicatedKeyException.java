package me.clipi.io.nbt.exceptions;

import me.clipi.io.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

public class NbtDuplicatedKeyException extends Exception {
	private static final long serialVersionUID = 6168204400609675503L;

	public final @NotNull NbtCompound compoundBeingConstructed;
	public final @NotNull String key;

	public NbtDuplicatedKeyException(@NotNull String key, @NotNull NbtCompound compoundBeingConstructed) {
		super("Key " + key + " is already present in the NBT Compound " + compoundBeingConstructed);
		this.key = key;
		this.compoundBeingConstructed = compoundBeingConstructed;
	}
}
