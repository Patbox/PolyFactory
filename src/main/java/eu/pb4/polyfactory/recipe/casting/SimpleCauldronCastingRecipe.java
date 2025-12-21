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
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public record SimpleCauldronCastingRecipe(FluidStack<?> fluidInput, ItemStack output, Holder<SoundEvent> soundEvent,
                                          double time, double coolingTime) implements CauldronCastingRecipe {
    public static final MapCodec<SimpleCauldronCastingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    FluidStack.CODEC.fieldOf("fluid_input").forGetter(SimpleCauldronCastingRecipe::fluidInput),
                    ItemStack.SINGLE_ITEM_CODEC.fieldOf("result").forGetter(SimpleCauldronCastingRecipe::output),
                    SoundEvent.CODEC.optionalFieldOf("sound", FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED).forGetter(SimpleCauldronCastingRecipe::soundEvent),
                    Codec.DOUBLE.fieldOf("time").forGetter(SimpleCauldronCastingRecipe::time),
                    Codec.DOUBLE.fieldOf("cooling_ticks").forGetter(SimpleCauldronCastingRecipe::coolingTime)
            ).apply(x, SimpleCauldronCastingRecipe::new)
    );

    public static SimpleCauldronCastingRecipe toItem(FluidStack<?> stack, Item out, SoundEvent sound, int coolingTime) {
        return new SimpleCauldronCastingRecipe(stack, out.getDefaultInstance(),
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), CastingRecipe.getTime(stack.instance(), stack.amount()), coolingTime);
    }

    @Override
    public double coolingTime(FluidContainerInput input) {
        return this.coolingTime;
    }

    @Override
    public boolean matches(FluidContainerInput input, Level world) {
        if (input.get(this.fluidInput.instance()) < this.fluidInput.amount()) {
            return false;
        }

        return true;
    }

    @Override
    public ItemStack assemble(FluidContainerInput input, HolderLookup.Provider lookup) {
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
