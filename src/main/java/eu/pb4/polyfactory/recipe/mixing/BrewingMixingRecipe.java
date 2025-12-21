package eu.pb4.polyfactory.recipe.mixing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public record BrewingMixingRecipe(String group,
                                  Ingredient ingredient,
                                  FluidInstance<?> from,
                                  FluidInstance<?> to,

                                  long minimumResult,
                                  long maxResult,
                                  double time,
                                  double minimumSpeed,
                                  double optimalSpeed, float minimumTemperature, float maxTemperature) implements MixingRecipe {
    public static final MapCodec<BrewingMixingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(BrewingMixingRecipe::group),
                    Ingredient.CODEC.fieldOf("ingredient").forGetter(BrewingMixingRecipe::ingredient),
                    FluidInstance.CODEC.fieldOf("from").forGetter(BrewingMixingRecipe::from),
                    FluidInstance.CODEC.fieldOf("to").forGetter(BrewingMixingRecipe::to),
                    Codec.LONG.fieldOf("min_amount").forGetter(BrewingMixingRecipe::minimumResult),
                    Codec.LONG.fieldOf("max_amount").forGetter(BrewingMixingRecipe::maxResult),
                    Codec.DOUBLE.fieldOf("time").forGetter(BrewingMixingRecipe::time),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 1d).forGetter(BrewingMixingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 1d).forGetter(BrewingMixingRecipe::optimalSpeed),
                    Codec.FLOAT.optionalFieldOf("minimal_temperature", -1f).forGetter(BrewingMixingRecipe::minimumTemperature),
                    Codec.FLOAT.optionalFieldOf("max_temperature", 2f).forGetter(BrewingMixingRecipe::maxTemperature)
            ).apply(x, BrewingMixingRecipe::new)
    );

    public Iterable<ItemStack> remainders(MixingInput input) {
        return List.of();
    }
    @Override
    public String group() {
        return this.group;
    }

    @Override
    public boolean matches(MixingInput inventory, Level world) {
        if (inventory.getFluid(this.from) < this.minimumResult) {
            return false;
        }


        for (var x : inventory.stacks()) {
            if (this.ingredient.test(x)) {
                return true;
            }
        }

        return false;
    }

    public void applyRecipeUse(MixerBlockEntity inventory, Level world) {
        var amount = Mth.clamp(inventory.getFluidContainer().get(this.from), this.minimumResult, this.maxResult);
        inventory.getFluidContainer().extract(this.from, amount, false);
        inventory.getFluidContainer().insert(this.to, amount, false);
        for (var i = MixerBlockEntity.INPUT_FIRST; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getItem(i);
            if (this.ingredient.test(stack)) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                break;
            }
        }
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
    public float minimumTemperature(MixingInput input) {
        return this.minimumTemperature;
    }

    @Override
    public float maxTemperature(MixingInput input) {
        return this.maxTemperature;
    }

    @Override
    public double time(MixingInput input) {
        return this.time;
    }

    @Override
    public ItemStack assemble(MixingInput inventory, HolderLookup.Provider registryManager) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<BrewingMixingRecipe> getSerializer() {
        return FactoryRecipeSerializers.MIXING_BREWING;
    }
}
