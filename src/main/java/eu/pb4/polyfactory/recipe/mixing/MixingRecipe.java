package eu.pb4.polyfactory.recipe.mixing;

import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.item.PolymerRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface MixingRecipe extends Recipe<MixerBlockEntity>, PolymerRecipe {

     Iterable<ItemStack> remainders();
     void applyRecipeUse(MixerBlockEntity inventory, World world);

    @Override
    default RecipeType<?> getType() {
        return FactoryRecipeTypes.MIXER;
    }

    double optimalSpeed();
    double minimumSpeed();
    float minimumTemperature();
    float maxTemperature();

    double time();
}
