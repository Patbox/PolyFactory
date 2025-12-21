package eu.pb4.polyfactory.recipe.mixing;

import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public interface MixingRecipe extends Recipe<MixingInput> {
     Iterable<ItemStack> remainders(MixingInput input);
     void applyRecipeUse(MixerBlockEntity inventory, Level world);

    @Override
    default RecipeType<MixingRecipe> getType() {
        return FactoryRecipeTypes.MIXER;
    }

    double optimalSpeed(MixingInput input);
    double minimumSpeed(MixingInput input);
    float minimumTemperature(MixingInput input);
    float maxTemperature(MixingInput input);

    double time(MixingInput input);
    default List<FluidStack<?>> fluidOutput(MixingInput input) {
        return List.of();
    }
    default List<FluidInputStack> fluidInput(MixingInput input) {
        return List.of();
    }

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CAMPFIRE;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }
}
