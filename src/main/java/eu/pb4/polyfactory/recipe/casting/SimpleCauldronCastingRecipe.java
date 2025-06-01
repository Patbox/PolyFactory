package eu.pb4.polyfactory.recipe.casting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polyfactory.recipe.input.SingleItemWithFluid;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

public record SimpleCauldronCastingRecipe(FluidStack<?> fluidInput, ItemStack output, RegistryEntry<SoundEvent> soundEvent,
                                          double time, double coolingTime) implements CauldronCastingRecipe {
    public static final MapCodec<SimpleCauldronCastingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    FluidStack.CODEC.fieldOf("fluid_input").forGetter(SimpleCauldronCastingRecipe::fluidInput),
                    ItemStack.UNCOUNTED_CODEC.fieldOf("result").forGetter(SimpleCauldronCastingRecipe::output),
                    SoundEvent.ENTRY_CODEC.optionalFieldOf("sound", FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED).forGetter(SimpleCauldronCastingRecipe::soundEvent),
                    Codec.DOUBLE.fieldOf("time").forGetter(SimpleCauldronCastingRecipe::time),
                    Codec.DOUBLE.fieldOf("cooling_ticks").forGetter(SimpleCauldronCastingRecipe::coolingTime)
            ).apply(x, SimpleCauldronCastingRecipe::new)
    );

    public static SimpleCauldronCastingRecipe toItem(FluidStack<?> stack, Item out, SoundEvent sound, int coolingTime) {
        return new SimpleCauldronCastingRecipe(stack, out.getDefaultStack(),
                Registries.SOUND_EVENT.getEntry(sound), CastingRecipe.getTime(stack.instance(), stack.amount()), coolingTime);
    }

    @Override
    public double coolingTime(FluidContainerInput input) {
        return this.coolingTime;
    }

    @Override
    public boolean matches(FluidContainerInput input, World world) {
        if (input.get(this.fluidInput.instance()) < this.fluidInput.amount()) {
            return false;
        }

        return true;
    }

    @Override
    public ItemStack craft(FluidContainerInput input, RegistryWrapper.WrapperLookup lookup) {
        return output.copy();
    }


    @Override
    public RecipeSerializer<SimpleCauldronCastingRecipe> getSerializer() {
        return FactoryRecipeSerializers.CASTING_CAULDRON_SIMPLE;
    }

    @Override
    public FluidStack<?> fluidInput(FluidContainerInput input) {
        return this.fluidInput;
    }

    @Override
    public double time(FluidContainerInput input) {
        return this.time;
    }
}
