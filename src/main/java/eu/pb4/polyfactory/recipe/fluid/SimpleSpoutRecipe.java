package eu.pb4.polyfactory.recipe.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import eu.pb4.polyfactory.recipe.input.SpoutInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.ComponentPredicate;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public record SimpleSpoutRecipe(Ingredient item, ComponentPredicate predicate, List<FluidStack<?>> fluidInput,
                                ItemStack output, RegistryEntry<SoundEvent> soundEvent, double maxSpeed, double time) implements SpoutRecipe {
    public static final MapCodec<SimpleSpoutRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("item").forGetter(SimpleSpoutRecipe::item),
                    ComponentPredicate.CODEC.fieldOf("item_predicate").forGetter(SimpleSpoutRecipe::predicate),
                    FluidStack.CODEC.listOf().optionalFieldOf("fluid_input", List.of()).forGetter(SimpleSpoutRecipe::fluidInput),
                    ItemStack.UNCOUNTED_CODEC.fieldOf("result").forGetter(SimpleSpoutRecipe::output),
                    SoundEvent.ENTRY_CODEC.fieldOf("sound").forGetter(SimpleSpoutRecipe::soundEvent),
                    Codec.DOUBLE.fieldOf("max_speed").forGetter(SimpleSpoutRecipe::maxSpeed),
                    Codec.DOUBLE.fieldOf("time").forGetter(SimpleSpoutRecipe::time)
            ).apply(x, SimpleSpoutRecipe::new)
    );
    

    public static SimpleSpoutRecipe toItem(Item item, FluidStack<?> stack, Item out, SoundEvent sound) {
        return new SimpleSpoutRecipe(Ingredient.ofItems(item), ComponentPredicate.EMPTY, List.of(stack), out.getDefaultStack(),
                Registries.SOUND_EVENT.getEntry(sound), SpoutRecipe.getMaxSpeed(stack.instance(), stack.amount()), SpoutRecipe.getTime(stack.instance(), stack.amount()));
    }

    @Override
    public boolean matches(SpoutInput input, World world) {
        if (!item.test(input.stack()) || !predicate.test(input.stack())) {
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
        return output.copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registriesLookup) {
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FactoryRecipeSerializers.SPOUT_SIMPLE;
    }

    @Override
    public List<FluidStack<?>> fluidInput(SpoutInput input) {
        return this.fluidInput;
    }

    @Override
    public double maxSpeed(SpoutInput input) {
        return this.maxSpeed;
    }

    @Override
    public double time(SpoutInput input) {
        return this.time;
    }
}
