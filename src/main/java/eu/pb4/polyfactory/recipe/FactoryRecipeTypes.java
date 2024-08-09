package eu.pb4.polyfactory.recipe;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.recipe.fluid.DrainRecipe;
import eu.pb4.polyfactory.recipe.fluid.SpoutRecipe;
import eu.pb4.polyfactory.recipe.mixing.MixingRecipe;
import eu.pb4.polyfactory.recipe.press.PressRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FactoryRecipeTypes {
    public static final RecipeType<GrindingRecipe> GRINDING = register("grinding");
    public static final RecipeType<PressRecipe> PRESS = register("press");
    public static final RecipeType<MixingRecipe> MIXER = register("mixer");
    public static final RecipeType<DrainRecipe> DRAIN = register("drain");
    public static final RecipeType<SpoutRecipe> SPOUT = register("spout");

    public static void register() {

    }

    public static <T extends Recipe<?>> RecipeType<T> register(String path) {
        return Registry.register(Registries.RECIPE_TYPE, Identifier.of(ModInit.ID, path), new RecipeType<T>() {
            @Override
            public String toString() {
                return ModInit.ID + ":" + path;
            }
        });
    }
}
