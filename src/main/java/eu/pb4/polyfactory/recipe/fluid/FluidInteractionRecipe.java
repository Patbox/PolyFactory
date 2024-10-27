package eu.pb4.polyfactory.recipe.fluid;

import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.recipe.IngredientPlacement;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategories;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface FluidInteractionRecipe extends Recipe<FluidContainerInput> {
    List<FluidInputStack> fluidInput(FluidContainerInput input, RegistryWrapper.WrapperLookup lookup);
    List<FluidStack<?>> fluidOutput(FluidContainerInput input, RegistryWrapper.WrapperLookup lookup);
    List<OutputStack> itemOutput(FluidContainerInput input, RegistryWrapper.WrapperLookup lookup);
    @Nullable
    ParticleEffect particle(FluidContainerInput input, Random random);

    @Nullable RegistryEntry<SoundEvent> soundEvent(FluidContainerInput input, Random random);

    int maxApplyPerTick();

    float particleChance(FluidContainerInput input);

    @Override
    default RecipeType<FluidInteractionRecipe> getType() {
        return FactoryRecipeTypes.FLUID_INTERACTION;
    }

    @Override
    default ItemStack craft(FluidContainerInput input, RegistryWrapper.WrapperLookup lookup) {
        return ItemStack.EMPTY;
    };

    @Override
    default RecipeBookCategory getRecipeBookCategory() {
        return RecipeBookCategories.CAMPFIRE;
    }

    @Override
    default boolean isIgnoredInRecipeBook() {
        return true;
    }

    @Override
    default IngredientPlacement getIngredientPlacement() {
        return IngredientPlacement.NONE;
    }
}
