package eu.pb4.factorytools.mixin.recipe;

import eu.pb4.factorytools.api.recipe.NbtRecipeBuilder;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.ShapedRecipe;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ShapedRecipeJsonBuilder.class)
public class ShapedRecipeJsonBuilderMixin implements NbtRecipeBuilder {
    @Unique
    @Nullable
    private NbtCompound nbt;

    @Override
    public void factorytools$setNbt(@Nullable NbtCompound nbt) {
        this.nbt = nbt;
    }

    @ModifyArg(method = "offerTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/data/server/recipe/RecipeExporter;accept(Lnet/minecraft/util/Identifier;Lnet/minecraft/recipe/Recipe;Lnet/minecraft/advancement/AdvancementEntry;)V"))
    private Recipe passNbt(Recipe t) {
        ((ShapedRecipe) t).getResult(null).setNbt(this.nbt);
        return t;
    }
}
