package eu.pb4.polyfactory.mixin.util;

import eu.pb4.polyfactory.util.DyeColorExtra;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DyeColor.class)
public class DyeColorMixin implements DyeColorExtra {
    @Unique
    private int polyfactory$color;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void polyfactory$storeColor(String string, int i, int id, String name, int color, MapColor mapColor, int fireworkColor, int signColor, CallbackInfo ci) {
        this.polyfactory$color = color;
        DyeColorExtra.BY_COLOR.put(color, (DyeColor) (Object) this);
    }

    @Override
    public int polyfactory$getColor() {
        return this.polyfactory$color;
    }
}
