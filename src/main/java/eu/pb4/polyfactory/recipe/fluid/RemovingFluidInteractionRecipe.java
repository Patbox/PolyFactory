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

public record RemovingFluidInteractionRecipe(List<FluidInputStack> fluidInput, Optional<ParticleOptions> particleEffect,
                                             Optional<Holder<SoundEvent>> soundEvent, float particleChance,
                                             float minTemperature, float maxTemperature) implements FluidInteractionRecipe {
    public static final MapCodec<RemovingFluidInteractionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FluidInputStack.CODEC.listOf().fieldOf("fluid_input").forGetter(RemovingFluidInteractionRecipe::fluidInput),
            ParticleTypes.CODEC.optionalFieldOf("particle").forGetter(RemovingFluidInteractionRecipe::particleEffect),
            SoundEvent.CODEC.optionalFieldOf("sound_event").forGetter(RemovingFluidInteractionRecipe::soundEvent),
            Codec.FLOAT.optionalFieldOf("particle_chance", 0f).forGetter(RemovingFluidInteractionRecipe::particleChance),
            Codec.FLOAT.optionalFieldOf("min_temperature", Float.NEGATIVE_INFINITY).forGetter(RemovingFluidInteractionRecipe::minTemperature),
            Codec.FLOAT.optionalFieldOf("max_temperature", Float.POSITIVE_INFINITY).forGetter(RemovingFluidInteractionRecipe::maxTemperature)
    ).apply(instance, RemovingFluidInteractionRecipe::new));

    @Override
    public List<FluidInputStack> fluidInput(FluidContainerInput input, HolderLookup.Provider lookup) {
        return this.fluidInput;
    }

    @Override
    public List<FluidStack<?>> fluidOutput(FluidContainerInput input, HolderLookup.Provider lookup) {
        return List.of();
    }

    @Override
    public List<OutputStack> itemOutput(FluidContainerInput input, HolderLookup.Provider lookup) {
        return List.of();
    }

    @Override
    public boolean matches(FluidContainerInput input, Level world) {
        if (input.temperature() < this.minTemperature || input.temperature() > this.maxTemperature) {
            return false;
        }

        int below = 0;

        for (var in : this.fluidInput) {
            var amount = input.get(in.instance());
            if (amount == 0) {
                return false;
            } else if (amount < in.required()) {
                below++;
            }
        }
        return below > 0;
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
    public int maxApplyPerTick() {
        return 1;
    }

    @Override
    public float particleChance(FluidContainerInput input) {
        return this.particleChance;
    }

    @Override
    public RecipeSerializer<RemovingFluidInteractionRecipe> getSerializer() {
        return FactoryRecipeSerializers.FLUID_INTERACTION_REMOVING;
    }
}
