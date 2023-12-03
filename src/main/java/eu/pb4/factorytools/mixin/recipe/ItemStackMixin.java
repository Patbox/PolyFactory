package eu.pb4.factorytools.mixin.recipe;

import com.mojang.serialization.Codec;
import eu.pb4.factorytools.impl.recipe.ExtendedCraftingResultCodec;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Shadow
    @Final
    @Mutable
    public static Codec<ItemStack> RECIPE_RESULT_CODEC;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void replaceCodec(CallbackInfo ci) {
        RECIPE_RESULT_CODEC = new ExtendedCraftingResultCodec(RECIPE_RESULT_CODEC);
    }
}
