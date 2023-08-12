package eu.pb4.polyfactory.mixin.nbtrecipe;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.recipe.ShapedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShapedRecipe.class)
public class ShapedRecipeMixin {
    @Inject(method = "outputFromJson", at = @At("RETURN"))
    private static void polyfactory$readNbt(JsonObject json, CallbackInfoReturnable<ItemStack> cir) {
        var jsonNbt = json.get("polyfactory:nbt");
        if (jsonNbt != null && jsonNbt.isJsonObject()) {
            cir.getReturnValue().setNbt(NbtCompound.CODEC.decode(JsonOps.COMPRESSED, jsonNbt).result().get().getFirst());
        }
    }
}
