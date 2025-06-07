package eu.pb4.polyfactory.mixin;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.storage.NbtReadView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NbtReadView.class)
public interface NbtReadViewAccessor {
    @Accessor
    NbtCompound getNbt();
}
