package eu.pb4.polyfactory.recipe.press;

import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polymer.core.api.item.PolymerRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.world.World;

public interface PressRecipe extends Recipe<PressBlockEntity>, PolymerRecipe {
    double minimumSpeed();

    void applyRecipeUse(PressBlockEntity inventory, World world);

    @Override
    default RecipeType<?> getType() {
        return FactoryRecipeTypes.PRESS;
    }
}
