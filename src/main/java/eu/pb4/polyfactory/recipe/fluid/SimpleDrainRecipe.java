package eu.pb4.polyfactory.recipe.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.DrainInput;
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

public record SimpleDrainRecipe(Ingredient item, ComponentPredicate predicate, Optional<Ingredient> catalyst, List<FluidStack<?>> fluidInput, List<FluidStack<?>> fluidOutput,
                                ItemStack output, RegistryEntry<SoundEvent> soundEvent, boolean requirePlayer) implements DrainRecipe {
    public static final MapCodec<SimpleDrainRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("item").forGetter(SimpleDrainRecipe::item),
                    ComponentPredicate.CODEC.fieldOf("item_predicate").forGetter(SimpleDrainRecipe::predicate),
                    Ingredient.ALLOW_EMPTY_CODEC.optionalFieldOf("catalyst").forGetter(SimpleDrainRecipe::catalyst),
                    FluidStack.CODEC.listOf().optionalFieldOf("fluid_input", List.of()).forGetter(SimpleDrainRecipe::fluidInput),
                    FluidStack.CODEC.listOf().optionalFieldOf("fluid_output", List.of()).forGetter(SimpleDrainRecipe::fluidOutput),
                    ItemStack.UNCOUNTED_CODEC.fieldOf("result").forGetter(SimpleDrainRecipe::output),
                    SoundEvent.ENTRY_CODEC.fieldOf("sound").forGetter(SimpleDrainRecipe::soundEvent),
                    Codec.BOOL.optionalFieldOf("require_player", false).forGetter(SimpleDrainRecipe::requirePlayer)
            ).apply(x, SimpleDrainRecipe::new)
    );

    public static SimpleDrainRecipe fromItem(Item item, FluidStack<?> stack, Item out, SoundEvent sound) {
        return new SimpleDrainRecipe(Ingredient.ofItems(item), ComponentPredicate.EMPTY, Optional.empty(), List.of(), List.of(stack), out.getDefaultStack(), Registries.SOUND_EVENT.getEntry(sound), false);
    }

    public static SimpleDrainRecipe toItem(Item item, FluidStack<?> stack, Item out, SoundEvent sound) {
        return new SimpleDrainRecipe(Ingredient.ofItems(item), ComponentPredicate.EMPTY, Optional.empty(), List.of(stack), List.of(), out.getDefaultStack(), Registries.SOUND_EVENT.getEntry(sound), true);
    }

    @Override
    public boolean matches(DrainInput input, World world) {
        if ((requirePlayer && !input.isPlayer()) || !item.test(input.stack()) || !predicate.test(input.stack()) || (catalyst.isPresent() && !catalyst.get().test(input.catalyst())) ) {
            return false;
        }
        for (var fluid : fluidInput) {
            if (input.getFluid(fluid.instance()) < fluid.amount()) {
                return false;
            }
        }

        var stored = input.stored();
        for (var fluid : fluidOutput) {
            stored += fluid.amount();
        }

        return input.capacity() >= stored;
    }

    @Override
    public ItemStack craft(DrainInput input, RegistryWrapper.WrapperLookup lookup) {
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
        return FactoryRecipeSerializers.DRAIN_SIMPLE;
    }

    @Override
    public List<FluidStack<?>> fluidOutput(DrainInput input) {
        return this.fluidOutput;
    }

    @Override
    public List<FluidStack<?>> fluidInput(DrainInput input) {
        return this.fluidInput;
    }
}
