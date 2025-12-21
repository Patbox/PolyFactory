package eu.pb4.polyfactory.recipe;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.recipe.casting.CastingRecipe;
import eu.pb4.polyfactory.recipe.casting.CauldronCastingRecipe;
import eu.pb4.polyfactory.recipe.drain.DrainRecipe;
import eu.pb4.polyfactory.recipe.fluid.FluidInteractionRecipe;
import eu.pb4.polyfactory.recipe.mixing.MixingRecipe;
import eu.pb4.polyfactory.recipe.press.PressRecipe;
import eu.pb4.polyfactory.recipe.smeltery.SmelteryRecipe;
import eu.pb4.polyfactory.recipe.spout.SpoutRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

public class FactoryRecipeTypes {
    public static final RecipeType<GrindingRecipe> GRINDING = register("grinding");
    public static final RecipeType<PressRecipe> PRESS = register("press");
    public static final RecipeType<MixingRecipe> MIXER = register("mixer");
    public static final RecipeType<DrainRecipe> DRAIN = register("drain");
    public static final RecipeType<SpoutRecipe> SPOUT = register("spout");
    public static final RecipeType<SmelteryRecipe> SMELTERY = register("smeltery");
    public static final RecipeType<CastingRecipe> CASTING = register("casting");
    public static final RecipeType<CauldronCastingRecipe> CASTING_CAULDRON = register("casting/cauldron");
    public static final RecipeType<FluidInteractionRecipe> FLUID_INTERACTION = register("fluid_interaction");

    public static void register() {

    }

    public static <T extends Recipe<?>> RecipeType<T> register(String path) {
        return Registry.register(BuiltInRegistries.RECIPE_TYPE, Identifier.fromNamespaceAndPath(ModInit.ID, path), new RecipeType<T>() {
            @Override
            public String toString() {
                return ModInit.ID + ":" + path;
            }
        });
    }
}
