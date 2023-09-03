package eu.pb4.polyfactory.mixin.recipe;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import eu.pb4.polyfactory.recipe.NbtRecipe;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShapedRecipeJsonBuilder.class)
public class ShapedRecipeJsonBuilderMixin implements NbtRecipe {
    @Unique
    @Nullable
    private NbtCompound polyfactory$nbt;

    @Override
    public void polyfactory$setNbt(@Nullable NbtCompound nbt) {
        polyfactory$nbt = nbt;
    }

    @ModifyArg(method = "offerTo", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private Object polyfactory$passNbt(Object t) {
        ((NbtRecipe) t).polyfactory$setNbt(this.polyfactory$nbt);
        return t;
    }

    @Mixin(targets = "net/minecraft/data/server/recipe/ShapedRecipeJsonBuilder$ShapedRecipeJsonProvider")
    public static class ShapedRecipeJsonProviderMixin implements NbtRecipe {
        @Unique
        @Nullable
        private NbtCompound polyfactory$nbt;

        @Override
        public void polyfactory$setNbt(@Nullable NbtCompound nbt) {
            polyfactory$nbt = nbt;
        }

        @Inject(method = "serialize", at = @At("TAIL"))
        private void polyfactory$serialize(JsonObject json, CallbackInfo ci) {
            if (polyfactory$nbt != null) {
                json.getAsJsonObject("result").add("polyfactory:nbt", NbtCompound.CODEC.encodeStart(JsonOps.COMPRESSED, this.polyfactory$nbt).result().get());
            }
        }
    }
}
