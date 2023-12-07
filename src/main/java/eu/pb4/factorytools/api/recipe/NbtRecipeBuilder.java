package eu.pb4.factorytools.api.recipe;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

public interface NbtRecipeBuilder {
    void factorytools$setNbt(@Nullable NbtCompound nbt);
}
