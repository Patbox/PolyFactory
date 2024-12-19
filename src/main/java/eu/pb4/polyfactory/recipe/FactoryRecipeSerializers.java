package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.MapCodec;
import eu.pb4.factorytools.api.recipe.LazyRecipeSerializer;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.recipe.drain.PotionAddDrainRecipe;
import eu.pb4.polyfactory.recipe.drain.PotionRemoveDrainRecipe;
import eu.pb4.polyfactory.recipe.fluid.RemovingFluidInteractionRecipe;
import eu.pb4.polyfactory.recipe.fluid.SimpleFluidInteractionRecipe;
import eu.pb4.polyfactory.recipe.spout.*;
import eu.pb4.polyfactory.recipe.mixing.*;
import eu.pb4.polyfactory.recipe.press.FillSprayCanPressRecipe;
import eu.pb4.polyfactory.recipe.press.GenericPressRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

public class FactoryRecipeSerializers {
    public static final LazyRecipeSerializer<GrindingRecipe> GRINDING = register("grinding", GrindingRecipe.CODEC);
    public static final LazyRecipeSerializer<GenericPressRecipe> PRESS_GENERIC = register("press/generic", GenericPressRecipe.CODEC);
    public static final LazyRecipeSerializer<ColoringMixingRecipe> MIXING_COLORING = register("mixing/coloring", ColoringMixingRecipe.CODEC);

    public static final LazyRecipeSerializer<GenericMixingRecipe> MIXING_GENERIC = register("mixing/generic", GenericMixingRecipe.CODEC);
    public static final LazyRecipeSerializer<TransformMixingRecipe> MIXING_TRANSFORM = register("mixing/transform", TransformMixingRecipe.CODEC);
    public static final LazyRecipeSerializer<FireworkStarMixingRecipe> MIXING_FIREWORK = register("mixing/firework", FireworkStarMixingRecipe.CODEC);
    public static final LazyRecipeSerializer<ArtificialDyeMixingRecipe> MIXING_ARTIFICIAL_DYE = register("mixing/artificial_dye", ArtificialDyeMixingRecipe.CODEC);
    public static final LazyRecipeSerializer<BrewingMixingRecipe> MIXING_BREWING = register("mixing/brewing", BrewingMixingRecipe.CODEC);
    public static final LazyRecipeSerializer<ColoringCraftingRecipe> CRAFTING_COLORING = register("crafting/coloring", ColoringCraftingRecipe.CODEC);
    public static final LazyRecipeSerializer<ShapelessNbtCopyRecipe> CRAFTING_SHAPELESS_NBT_COPY = register("crafting/shapeless_nbt_copy", ShapelessNbtCopyRecipe.CODEC);

    public static final LazyRecipeSerializer<PRTKeySetterCraftingRecipe> CRAFTING_PRT_KEY_SETTER = register("crafting/prt_key_setter", PRTKeySetterCraftingRecipe.CODEC);

    public static final LazyRecipeSerializer<FillSprayCanCraftingRecipe> CRAFTING_FILL_SPRAY_CAN = register("crafting/fill_spray_can",
            CraftingRecipeCategory.CODEC.xmap(FillSprayCanCraftingRecipe::new, FillSprayCanCraftingRecipe::category).fieldOf("category"));
    public static final LazyRecipeSerializer<FillSprayCanPressRecipe> PRESS_FILL_SPRAY_CAN = register("press/fill_spray_can",
            Codecs.POSITIVE_INT.xmap(FillSprayCanPressRecipe::new, FillSprayCanPressRecipe::amount).fieldOf("amount"));

    public static final LazyRecipeSerializer<SimpleDrainRecipe> DRAIN_SIMPLE = register("drain/simple", SimpleDrainRecipe.CODEC);
    public static final LazyRecipeSerializer<PotionAddDrainRecipe> DRAIN_POTION_ADD = register("drain/potion_add", PotionAddDrainRecipe.CODEC);
    public static final LazyRecipeSerializer<PotionRemoveDrainRecipe> DRAIN_POTION_REMOVE = register("drain/potion_remove", PotionRemoveDrainRecipe.CODEC);
    public static final LazyRecipeSerializer<SimpleSpoutRecipe> SPOUT_SIMPLE = register("spout/simple", SimpleSpoutRecipe.CODEC);
    public static final LazyRecipeSerializer<PotionSpoutRecipe> SPOUT_POTION = register("spout/potion", PotionSpoutRecipe.CODEC);
    public static final LazyRecipeSerializer<RepairSpoutRecipe> SPOUT_EXPERIENCE_REPAIR = register("spout/experience_repair", RepairSpoutRecipe.CODEC);
    public static final LazyRecipeSerializer<SimpleFluidInteractionRecipe> FLUID_INTERACTION_SIMPLE = register("fluid_interaction/simple", SimpleFluidInteractionRecipe.CODEC);
    public static final LazyRecipeSerializer<RemovingFluidInteractionRecipe> FLUID_INTERACTION_REMOVING = register("fluid_interaction/removing", RemovingFluidInteractionRecipe.CODEC);

    public static void register() {

    }

    public static <T extends RecipeSerializer<?>> T register(String path, T recipeSerializer) {
        return Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(ModInit.ID, path), recipeSerializer);
    }

    public static <T extends Recipe<?>> LazyRecipeSerializer<T> register(String path, MapCodec<T> codec) {
        return Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(ModInit.ID, path), new LazyRecipeSerializer<>(codec));
    }
}
