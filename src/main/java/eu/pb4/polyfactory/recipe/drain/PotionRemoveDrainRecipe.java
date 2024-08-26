package eu.pb4.polyfactory.recipe.drain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.spout.SpoutRecipe;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public record PotionRemoveDrainRecipe(Ingredient item, Optional<Ingredient> catalyst, long amount, ItemStack output, RegistryEntry<SoundEvent> soundEvent,
                                      boolean requirePlayer, double time) implements DrainRecipe {
    public static final MapCodec<PotionRemoveDrainRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("item").forGetter(PotionRemoveDrainRecipe::item),
                    Ingredient.ALLOW_EMPTY_CODEC.optionalFieldOf("catalyst").forGetter(PotionRemoveDrainRecipe::catalyst),
                    Codec.LONG.fieldOf("amount").forGetter(PotionRemoveDrainRecipe::amount),
                    ItemStack.UNCOUNTED_CODEC.fieldOf("result").forGetter(PotionRemoveDrainRecipe::output),
                    SoundEvent.ENTRY_CODEC.fieldOf("sound").forGetter(PotionRemoveDrainRecipe::soundEvent),
                    Codec.BOOL.optionalFieldOf("require_player", false).forGetter(PotionRemoveDrainRecipe::requirePlayer),
                    Codec.DOUBLE.fieldOf("time").forGetter(PotionRemoveDrainRecipe::time)
            ).apply(x, PotionRemoveDrainRecipe::new)
    );

    public static PotionRemoveDrainRecipe of(Item item, long amount, Item result, SoundEvent sound) {
        return new PotionRemoveDrainRecipe(Ingredient.ofItems(item), Optional.empty(), amount, result.getDefaultStack(), Registries.SOUND_EVENT.getEntry(sound),
                true, SpoutRecipe.getTime(FactoryFluids.POTION.defaultInstance(), amount));
    }

    @Override
    public boolean matches(DrainInput input, World world) {
        if ((requirePlayer && !input.isPlayer()) ||!item.test(input.stack()) || (catalyst.isPresent() && !catalyst.get().test(input.catalyst()))) {
            return false;
        }

        for (var key : input.fluids()) {
            if ((key.type() == FactoryFluids.POTION || key.type() == FactoryFluids.WATER) && input.getFluid(key) >= amount) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ItemStack craft(DrainInput input, RegistryWrapper.WrapperLookup lookup) {
        for (var key : input.fluids()) {
            if ((key.type() == FactoryFluids.POTION || key.type() == FactoryFluids.WATER) && input.getFluid(key) >= amount) {
                var stack = this.output.copy();
                stack.set(DataComponentTypes.POTION_CONTENTS, key.type() == FactoryFluids.POTION
                        ? (PotionContentsComponent) key.data()
                        : PotionContentsComponent.DEFAULT.with(Potions.WATER));
                return stack;
            }
        }

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
        return FactoryRecipeSerializers.DRAIN_POTION_REMOVE;
    }

    @Override
    public List<FluidStack<?>> fluidOutput(DrainInput input) {
        return List.of();
    }

    @Override
    public List<FluidStack<?>> fluidInput(DrainInput input) {
        for (var key : input.fluids()) {
            if ((key.type() == FactoryFluids.POTION || key.type() == FactoryFluids.WATER) && input.getFluid(key) >= amount) {
                return List.of(key.stackOf(this.amount));
            }
        }
        return List.of();
    }

    @Override
    public double time(DrainInput input) {
        return this.time;
    }
}
