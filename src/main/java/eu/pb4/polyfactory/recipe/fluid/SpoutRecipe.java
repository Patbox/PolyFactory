package eu.pb4.polyfactory.recipe.fluid;

import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import eu.pb4.polyfactory.recipe.input.SpoutInput;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;

import java.util.List;

public interface SpoutRecipe extends Recipe<SpoutInput> {
    static double getMaxSpeed(FluidInstance<?> fluidInstance, long amount) {
        return fluidInstance.getMaxFlow(null) / 1.5d;
    }

    static double getTime(FluidInstance<?> fluidInstance, long amount) {
        return Math.max(amount / fluidInstance.getFlowSpeedMultiplier(null), 1);
    }

    List<FluidStack<?>> fluidInput(SpoutInput input);

    RegistryEntry<SoundEvent> soundEvent();
    double maxSpeed(SpoutInput input);
    double time(SpoutInput input);
    @Override
    default RecipeType<?> getType() {
        return FactoryRecipeTypes.SPOUT;
    };
}
