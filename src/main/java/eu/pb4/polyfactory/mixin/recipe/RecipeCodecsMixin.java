package eu.pb4.polyfactory.mixin.recipe;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.recipe.ExtendedCraftingResultCodec;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeCodecs.class)
public class RecipeCodecsMixin {
    @Shadow
    @Final
    @Mutable
    public static Codec<ItemStack> CRAFTING_RESULT;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void polyfactory$readNbt(CallbackInfo ci) {
        CRAFTING_RESULT = new ExtendedCraftingResultCodec(CRAFTING_RESULT);
    }
}
