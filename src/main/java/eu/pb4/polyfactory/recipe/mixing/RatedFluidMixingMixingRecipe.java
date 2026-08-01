package eu.pb4.polyfactory.recipe.mixing;

import com.google.common.collect.Iterators;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.factorytools.api.util.ItemComponentPredicate;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import eu.pb4.polyfactory.util.FactoryUtil;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToLongFunction;

public record RatedFluidMixingMixingRecipe(String group, List<FluidInputStack> fluidInputs,
                                           List<FluidStack<?>> fluidOutput, int maxMultiplied,
                                           double time,
                                           double minimumSpeed,
                                           double optimalSpeed, float minimumTemperature, float maxTemperature) implements MixingRecipe {
    public static final MapCodec<RatedFluidMixingMixingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(RatedFluidMixingMixingRecipe::group),
                    ExtraCodecs.compactListCodec(FluidInputStack.CODEC).fieldOf("input").forGetter(RatedFluidMixingMixingRecipe::fluidInputs),
                    ExtraCodecs.compactListCodec(FluidStack.CODEC).fieldOf("output").forGetter(RatedFluidMixingMixingRecipe::fluidOutput),
                    Codec.INT.optionalFieldOf("max_multiplied", 1).forGetter(RatedFluidMixingMixingRecipe::maxMultiplied),
                    Codec.DOUBLE.fieldOf("time").forGetter(RatedFluidMixingMixingRecipe::time),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 1d).forGetter(RatedFluidMixingMixingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 1d).forGetter(RatedFluidMixingMixingRecipe::optimalSpeed),
                    Codec.FLOAT.optionalFieldOf("minimal_temperature", Float.NEGATIVE_INFINITY).forGetter(RatedFluidMixingMixingRecipe::minimumTemperature),
                    Codec.FLOAT.optionalFieldOf("max_temperature", Float.POSITIVE_INFINITY).forGetter(RatedFluidMixingMixingRecipe::maxTemperature)
            ).apply(x, RatedFluidMixingMixingRecipe::new)
    );

    public static RecipeHolder<RatedFluidMixingMixingRecipe> of(String string, String group, List<FluidInputStack> fluidInput, double mixingTime, double minimumSpeed, double optimalSpeed, double minTemperature, double maxTemperature, int maxMultiple, List<FluidStack<?>> fluidOutput) {
        return new RecipeHolder<>(FactoryUtil.recipeKey("mixing/" + string), new RatedFluidMixingMixingRecipe(group,
                fluidInput, fluidOutput, maxMultiple,
                mixingTime, minimumSpeed, optimalSpeed, (float) minTemperature, (float) maxTemperature));
    }

    @Override
    public String group() {
        return this.group;
    }

    @Override
    public boolean matches(MixingInput inventory, Level world) {
        for (var fluid : this.fluidInputs) {
            if (inventory.getFluid(fluid.instance()) < fluid.required()) {
                return false;
            }
        }

        if (!inventory.isSlotEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public Iterable<ItemStack> remainders(MixingInput input) {
        return List.of();
    }

    public void applyRecipeUse(MixerBlockEntity inventory, Level world) {
        var container = inventory.getFluidContainer();

        var mult = this.findMultiplier(container::get);

        for (var fluid : this.fluidInputs) {
            container.extract(fluid.instance(), fluid.used() * mult, false);
        }
    }

    private int findMultiplier(ToLongFunction<FluidInstance<?>> container) {
        long mult = this.maxMultiplied;

        for (var fluid : this.fluidInputs) {
            mult = Math.min(container.applyAsLong(fluid.instance()) / fluid.required(), mult);
        }

        return Math.max(Math.toIntExact(mult), 1);
    }

    @Override
    public List<ItemStack> assembleStacks(MixingInput input, RandomSource randomSource, boolean applyChance) {
        return List.of();
    }

    @Override
    public RecipeSerializer<RatedFluidMixingMixingRecipe> getSerializer() {
        return FactoryRecipeSerializers.MIXING_RATED_FLUID;
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
    public List<FluidStack<?>> fluidOutput(MixingInput input) {
        var mult = this.findMultiplier(input.fluidContainer()::get);

        //noinspection unchecked
        return (List<FluidStack<?>>) (Object) this.fluidOutput.stream().map(s -> s.withAmount(s.amount() * mult)).toList();
    }

    @Override
    public List<FluidInputStack> fluidInput(MixingInput input) {
        var mult = this.findMultiplier(input.fluidContainer()::get);

        return this.fluidInputs.stream().map(s -> s.scale(mult)).toList();
    }

    @Override
    public double time(MixingInput input) {
        return this.time;
    }
}
