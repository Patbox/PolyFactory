package eu.pb4.polyfactory.recipe.mixing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.mixin.BrewingRecipeRegistryAccessor;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;

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
    public String getGroup() {
        return this.group;
    }

    @Override
    public boolean matches(MixingInput inventory, World world) {
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

    public void applyRecipeUse(MixerBlockEntity inventory, World world) {
        var amount = MathHelper.clamp(inventory.getFluidContainer().get(this.from), this.minimumResult, this.maxResult);
        inventory.getFluidContainer().extract(this.from, amount, false);
        inventory.getFluidContainer().insert(this.to, amount, false);
        for (var i = MixerBlockEntity.INPUT_FIRST; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getStack(i);
            if (this.ingredient.test(stack)) {
                stack.decrement(1);
                if (stack.isEmpty()) {
                    inventory.setStack(i, ItemStack.EMPTY);
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
    public ItemStack craft(MixingInput inventory, RegistryWrapper.WrapperLookup registryManager) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<BrewingMixingRecipe> getSerializer() {
        return FactoryRecipeSerializers.MIXING_BREWING;
    }
}
