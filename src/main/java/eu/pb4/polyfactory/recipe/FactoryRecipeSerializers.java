package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.recipe.mixing.ArtificialDyeMixingRecipe;
import eu.pb4.polyfactory.recipe.mixing.ColoringMixingRecipe;
import eu.pb4.polyfactory.recipe.mixing.FireworkStarMixingRecipe;
import eu.pb4.polyfactory.recipe.mixing.GenericMixingRecipe;
import eu.pb4.polyfactory.recipe.press.GenericPressRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FactoryRecipeSerializers {
    public static final CodecRecipeSerializer<GrindingRecipe> GRINDING = register("grinding", GrindingRecipe.CODEC);
    public static final CodecRecipeSerializer<GenericPressRecipe> PRESS_GENERIC = register("press/generic", GenericPressRecipe.CODEC);
    public static final CodecRecipeSerializer<ColoringMixingRecipe> MIXING_COLORING = register("mixing/coloring", ColoringMixingRecipe.CODEC);

    public static final CodecRecipeSerializer<GenericMixingRecipe> MIXING_GENERIC = register("mixing/generic", GenericMixingRecipe.CODEC);
    public static final CodecRecipeSerializer<FireworkStarMixingRecipe> MIXING_FIREWORK = register("mixing/firework", FireworkStarMixingRecipe.CODEC);
    public static final CodecRecipeSerializer<ArtificialDyeMixingRecipe> MIXING_ARTIFICIAL_DYE = register("mixing/artificial_dye", ArtificialDyeMixingRecipe.CODEC);
    public static final CodecRecipeSerializer<ColoringCraftingRecipe> CRAFTING_COLORING = register("crafting/coloring", ColoringCraftingRecipe.CODEC);

    public static void register() {

    }

    public static <T extends RecipeSerializer<?>> T register(String path, T recipeSerializer) {
        return Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(ModInit.ID, path), recipeSerializer);
    }

    public static <T extends Recipe<?>> CodecRecipeSerializer<T> register(String path, Codec<T> codec) {
        return Registry.register(Registries.RECIPE_SERIALIZER, new Identifier(ModInit.ID, path), new CodecRecipeSerializer<>(codec));
    }
}
