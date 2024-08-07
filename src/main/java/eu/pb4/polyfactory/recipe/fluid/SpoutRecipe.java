package eu.pb4.polyfactory.recipe.fluid;

import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

import java.util.List;

public interface SpoutRecipe extends Recipe<DrainInput> {
    List<FluidStack<?>> fluidOutput(DrainInput input);
    List<FluidStack<?>> fluidInput(DrainInput input);

    RegistryEntry<SoundEvent> soundEvent();

    default boolean requirePlayer() {
        return false;
    }

    @Override
    default RecipeType<?> getType() {
        return FactoryRecipeTypes.DRAIN;
    };
}
