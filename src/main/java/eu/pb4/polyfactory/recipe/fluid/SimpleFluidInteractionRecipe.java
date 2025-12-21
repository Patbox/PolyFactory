package eu.pb4.polyfactory.recipe.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polyfactory.recipe.input.FluidInputStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public record SimpleFluidInteractionRecipe(List<FluidInputStack> fluidInput, List<FluidStack<?>> fluidOutput,
                                           List<OutputStack> itemOutput, Optional<ParticleOptions> particleEffect,
                                           Optional<Holder<SoundEvent>> soundEvent, float particleChance,
                                           float minTemperature, float maxTemperature, int maxApplyPerTick) implements FluidInteractionRecipe {
    public static final MapCodec<SimpleFluidInteractionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FluidInputStack.CODEC.listOf().fieldOf("fluid_input").forGetter(SimpleFluidInteractionRecipe::fluidInput),
            FluidStack.CODEC.listOf().fieldOf("fluid_output").forGetter(SimpleFluidInteractionRecipe::fluidOutput),
            OutputStack.CODEC.listOf().fieldOf("item_output").forGetter(SimpleFluidInteractionRecipe::itemOutput),
            ParticleTypes.CODEC.optionalFieldOf("particle").forGetter(SimpleFluidInteractionRecipe::particleEffect),
            SoundEvent.CODEC.optionalFieldOf("sound_event").forGetter(SimpleFluidInteractionRecipe::soundEvent),
            Codec.FLOAT.optionalFieldOf("particle_chance", 0f).forGetter(SimpleFluidInteractionRecipe::particleChance),
            Codec.FLOAT.optionalFieldOf("min_temperature", Float.NEGATIVE_INFINITY).forGetter(SimpleFluidInteractionRecipe::minTemperature),
            Codec.FLOAT.optionalFieldOf("max_temperature", Float.POSITIVE_INFINITY).forGetter(SimpleFluidInteractionRecipe::maxTemperature),
            Codec.INT.optionalFieldOf("max_apply_per_tick", 1).forGetter(SimpleFluidInteractionRecipe::maxApplyPerTick)
    ).apply(instance, SimpleFluidInteractionRecipe::new));

    @Override
    public List<FluidInputStack> fluidInput(FluidContainerInput input, HolderLookup.Provider lookup) {
        return fluidInput;
    }

    @Override
    public List<FluidStack<?>> fluidOutput(FluidContainerInput input, HolderLookup.Provider lookup) {
        return fluidOutput;
    }

    @Override
    public List<OutputStack> itemOutput(FluidContainerInput input, HolderLookup.Provider lookup) {
        return itemOutput;
    }

    @Override
    public boolean matches(FluidContainerInput input, Level world) {
        if (input.temperature() < this.minTemperature || input.temperature() > this.maxTemperature) {
            return false;
        }

        var empty = input.capacity() - input.stored();
        for (var in : this.fluidInput) {
            if (input.get(in.instance()) < in.required()) {
                return false;
            }
            empty += in.used();
        }

        for (var in : this.fluidOutput) {
            empty -= in.amount();
        }

        return empty >= 0;
    }

    @Override
    public @Nullable ParticleOptions particle(FluidContainerInput input, RandomSource random) {
        return this.particleEffect.orElse(null);
    }

    @Override
    public @Nullable Holder<SoundEvent> soundEvent(FluidContainerInput input, RandomSource random) {
        return this.soundEvent.orElse(null);
    }

    @Override
    public float particleChance(FluidContainerInput input) {
        return this.particleChance;
    }

    @Override
    public RecipeSerializer<SimpleFluidInteractionRecipe> getSerializer() {
        return FactoryRecipeSerializers.FLUID_INTERACTION_SIMPLE;
    }
}
