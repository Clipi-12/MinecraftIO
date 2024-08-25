package me.clipi.io.nbt.exceptions;

import me.clipi.io.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

public class NbtKeyNotFoundException extends Exception {
	private static final long serialVersionUID = -3525208150628883798L;

	public final @NotNull NbtCompound compoundBeingConstructed;
	public final @NotNull String key;

	public NbtKeyNotFoundException(@NotNull String key, @NotNull NbtCompound compound) {
		super("Key " + key + " is not present in the NBT Compound " + compound);
		this.key = key;
		this.compoundBeingConstructed = compound;
	}
}
