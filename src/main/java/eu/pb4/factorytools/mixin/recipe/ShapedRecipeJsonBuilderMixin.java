package eu.pb4.factorytools.mixin.recipe;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import eu.pb4.factorytools.api.recipe.NbtRecipe;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
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
    private NbtCompound nbt;

    @Override
    public void factorytools$setNbt(@Nullable NbtCompound nbt) {
        this.nbt = nbt;
    }

    @ModifyArg(method = "offerTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/data/server/recipe/RecipeExporter;accept(Lnet/minecraft/data/server/recipe/RecipeJsonProvider;)V"))
    private RecipeJsonProvider passNbt(RecipeJsonProvider t) {
        ((NbtRecipe) t).factorytools$setNbt(this.nbt);
        return t;
    }

    @Mixin(targets = "net/minecraft/data/server/recipe/ShapedRecipeJsonBuilder$ShapedRecipeJsonProvider")
    public static class ShapedRecipeJsonProviderMixin implements NbtRecipe {
        @Unique
        @Nullable
        private NbtCompound nbt;

        @Override
        public void factorytools$setNbt(@Nullable NbtCompound nbt) {
            this.nbt = nbt;
        }

        @Inject(method = "serialize", at = @At("TAIL"))
        private void serializeNbt(JsonObject json, CallbackInfo ci) {
            if (nbt != null) {
                json.getAsJsonObject("result").add("factorytools:nbt", NbtCompound.CODEC.encodeStart(JsonOps.COMPRESSED, this.nbt).result().get());
            }
        }
    }
}
