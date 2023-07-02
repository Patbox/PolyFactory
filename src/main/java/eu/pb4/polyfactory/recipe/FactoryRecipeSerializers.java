package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.ModInit;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FactoryRecipeSerializers {
    public static final CodecRecipeSerializer<GrindingRecipe> GRINDING = register("grinding", GrindingRecipe.CODEC);
    public static final CodecRecipeSerializer<PressRecipe> PRESS = register("press", PressRecipe.CODEC);
    public static final CodecRecipeSerializer<MixingRecipe> MIXING = register("mixing", MixingRecipe.CODEC);

    public static void register() {

    }

    public static <T extends RecipeSerializer<?>> T register(String path, T recipeSerializer) {
        return Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(ModInit.ID, path), recipeSerializer);
    }

    public static <T extends Recipe<?>> CodecRecipeSerializer<T> register(String path, Codec<T> codec) {
        return Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(ModInit.ID, path), new CodecRecipeSerializer<>(codec));
    }
}
