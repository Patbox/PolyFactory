package eu.pb4.polyfactory.recipe.spout;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.SpoutInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Unit;
import net.minecraft.world.World;

import java.util.List;

public record SimpleSpoutRecipe(CountedIngredient item, List<FluidStack<?>> fluidInput,
                                ItemStack output, boolean copyComponents, RegistryEntry<SoundEvent> soundEvent, double time) implements SpoutRecipe {
    public static final MapCodec<SimpleSpoutRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    CountedIngredient.CODEC.fieldOf("item").forGetter(SimpleSpoutRecipe::item),
                    FluidStack.CODEC.listOf().optionalFieldOf("fluid_input", List.of()).forGetter(SimpleSpoutRecipe::fluidInput),
                    ItemStack.UNCOUNTED_CODEC.fieldOf("result").forGetter(SimpleSpoutRecipe::output),
                    Codec.BOOL.optionalFieldOf("copy_components", false).forGetter(SimpleSpoutRecipe::copyComponents),
                    SoundEvent.ENTRY_CODEC.fieldOf("sound").forGetter(SimpleSpoutRecipe::soundEvent),
                    Codec.DOUBLE.fieldOf("time").forGetter(SimpleSpoutRecipe::time)
            ).apply(x, SimpleSpoutRecipe::new)
    );
    

    public static SimpleSpoutRecipe toItem(Item item, FluidStack<?> stack, Item out, SoundEvent sound) {
        return new SimpleSpoutRecipe(CountedIngredient.ofItems(1, item), List.of(stack), out.getDefaultStack(), false,
                Registries.SOUND_EVENT.getEntry(sound), SpoutRecipe.getTime(stack.instance(), stack.amount()));
    }

    public static SimpleSpoutRecipe toItemCopy(Item item, FluidStack<?> stack, Item out, SoundEvent sound) {
        return new SimpleSpoutRecipe(CountedIngredient.ofItems(1, item), List.of(stack), out.getDefaultStack(), true,
                Registries.SOUND_EVENT.getEntry(sound), SpoutRecipe.getTime(stack.instance(), stack.amount()));
    }

    public static SimpleSpoutRecipe template(Item template, FluidStack<Unit> stack, Item out, SoundEvent sound) {
        return new SimpleSpoutRecipe(CountedIngredient.ofItems(0, template), List.of(stack), out.getDefaultStack(), false,
                Registries.SOUND_EVENT.getEntry(sound), SpoutRecipe.getTime(stack.instance(), stack.amount()));
    }

    @Override
    public int decreasedInputItemAmount(SpoutInput input) {
        return this.item.count();
    }

    @Override
    public boolean matches(SpoutInput input, World world) {
        if (!item.test(input.stack())) {
            return false;
        }
        for (var fluid : fluidInput) {
            if (input.getFluid(fluid.instance()) < fluid.amount()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack craft(SpoutInput input, RegistryWrapper.WrapperLookup lookup) {
        var out = output.copy();
        if (this.copyComponents) {
            out.applyChanges(input.stack().getComponentChanges());
        }

        return out;
    }


    @Override
    public RecipeSerializer<SimpleSpoutRecipe> getSerializer() {
        return FactoryRecipeSerializers.SPOUT_SIMPLE;
    }

    @Override
    public List<FluidStack<?>> fluidInput(SpoutInput input) {
        return this.fluidInput;
    }

    @Override
    public double time(SpoutInput input) {
        return this.time;
    }
}
