package eu.pb4.polyfactory.recipe;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

public interface NbtRecipe {
    void polyfactory$setNbt(@Nullable NbtCompound nbt);
}
