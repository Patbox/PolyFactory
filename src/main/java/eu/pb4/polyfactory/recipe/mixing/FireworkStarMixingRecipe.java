package eu.pb4.polyfactory.recipe.mixing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import java.util.Collections;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.FireworkStarRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public record FireworkStarMixingRecipe(double time,
                                       double minimumSpeed,
                                       double optimalSpeed) implements MixingRecipe {
    public static final FireworkStarRecipe VANILLA = new FireworkStarRecipe(CraftingBookCategory.BUILDING);

    public static final MapCodec<FireworkStarMixingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.DOUBLE.fieldOf("time").forGetter(FireworkStarMixingRecipe::time),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 1d).forGetter(FireworkStarMixingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 1d).forGetter(FireworkStarMixingRecipe::optimalSpeed)
            ).apply(x, FireworkStarMixingRecipe::new)
    );

    @Override
    public Iterable<ItemStack> remainders(MixingInput input) {
        return Collections.emptyList();
    }

    @Override
    public void applyRecipeUse(MixerBlockEntity inventory, Level world) {
        for (int i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public float minimumTemperature(MixingInput input) {
        return 0;
    }

    @Override
    public float maxTemperature(MixingInput input) {
        return 0.3f;
    }

    @Override
    public boolean matches(MixingInput inventory, Level world) {
        if (!inventory.fluids().isEmpty()) {
            return false;
        }
        return VANILLA.matches(inventory.asCraftingRecipeInput(), world);
    }

    @Override
    public ItemStack assemble(MixingInput inventory, HolderLookup.Provider registryManager) {
        return VANILLA.assemble(inventory.asCraftingRecipeInput(), registryManager);
    }

    @Override
    public RecipeSerializer<FireworkStarMixingRecipe> getSerializer() {
        return FactoryRecipeSerializers.MIXING_FIREWORK;
    }

    @Override
    public double optimalSpeed(MixingInput input) {
        return this.optimalSpeed;
    }

    @Override
    public double minimumSpeed(MixingInput input) {
        return this.minimumSpeed;
    }

    @Override
    public double time(MixingInput input) {
        return this.time;
    }
}
