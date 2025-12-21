package eu.pb4.polyfactory.recipe.fluid;

import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeType;

public interface FluidInteractionRecipe extends Recipe<FluidContainerInput> {
    List<FluidInputStack> fluidInput(FluidContainerInput input, HolderLookup.Provider lookup);
    List<FluidStack<?>> fluidOutput(FluidContainerInput input, HolderLookup.Provider lookup);
    List<OutputStack> itemOutput(FluidContainerInput input, HolderLookup.Provider lookup);
    @Nullable
    ParticleOptions particle(FluidContainerInput input, RandomSource random);

    @Nullable Holder<SoundEvent> soundEvent(FluidContainerInput input, RandomSource random);

    int maxApplyPerTick();

    float particleChance(FluidContainerInput input);

    @Override
    default RecipeType<FluidInteractionRecipe> getType() {
        return FactoryRecipeTypes.FLUID_INTERACTION;
    }

    @Override
    default ItemStack assemble(FluidContainerInput input, HolderLookup.Provider lookup) {
        return ItemStack.EMPTY;
    };

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
