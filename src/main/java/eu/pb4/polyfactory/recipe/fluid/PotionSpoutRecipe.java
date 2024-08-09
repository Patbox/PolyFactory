package eu.pb4.polyfactory.recipe.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.SpoutInput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public record PotionSpoutRecipe(Ingredient item, long amount, ItemStack output, RegistryEntry<SoundEvent> soundEvent) implements SpoutRecipe {
    public static final MapCodec<PotionSpoutRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("item").forGetter(PotionSpoutRecipe::item),
                    Codec.LONG.fieldOf("amount").forGetter(PotionSpoutRecipe::amount),
                    ItemStack.UNCOUNTED_CODEC.fieldOf("result").forGetter(PotionSpoutRecipe::output),
                    SoundEvent.ENTRY_CODEC.fieldOf("sound").forGetter(PotionSpoutRecipe::soundEvent)
            ).apply(x, PotionSpoutRecipe::new)
    );

    @Override
    public boolean matches(SpoutInput input, World world) {
        if (!item.test(input.stack())) {
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
    public ItemStack craft(SpoutInput input, RegistryWrapper.WrapperLookup lookup) {
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
        return FactoryRecipeSerializers.SPOUT_POTION;
    }
    @Override
    public List<FluidStack<?>> fluidInput(SpoutInput input) {
        for (var key : input.fluids()) {
            if ((key.type() == FactoryFluids.POTION || key.type() == FactoryFluids.WATER) && input.getFluid(key) >= amount) {
                return List.of(key.stackOf(this.amount));
            }
        }
        return List.of();
    }
}
