package eu.pb4.polyfactory.recipe.fluid;

import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import eu.pb4.polyfactory.recipe.input.SpoutInput;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

import java.util.List;

public interface SpoutRecipe extends Recipe<SpoutInput> {
    List<FluidStack<?>> fluidInput(SpoutInput input);

    RegistryEntry<SoundEvent> soundEvent();

    @Override
    default RecipeType<?> getType() {
        return FactoryRecipeTypes.SPOUT;
    };
}
