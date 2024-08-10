package eu.pb4.polyfactory.recipe.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
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

public record PotionAddDrainRecipe(Ingredient item, Optional<Ingredient> catalyst, long amount, ItemStack output, RegistryEntry<SoundEvent> soundEvent,
                                   boolean requirePlayer, double maxSpeed, double time) implements DrainRecipe {
    public static final MapCodec<PotionAddDrainRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("item").forGetter(PotionAddDrainRecipe::item),
                    Ingredient.ALLOW_EMPTY_CODEC.optionalFieldOf("catalyst").forGetter(PotionAddDrainRecipe::catalyst),
                    Codec.LONG.fieldOf("amount").forGetter(PotionAddDrainRecipe::amount),
                    ItemStack.UNCOUNTED_CODEC.fieldOf("result").forGetter(PotionAddDrainRecipe::output),
                    SoundEvent.ENTRY_CODEC.fieldOf("sound").forGetter(PotionAddDrainRecipe::soundEvent),
                    Codec.BOOL.optionalFieldOf("require_player", false).forGetter(PotionAddDrainRecipe::requirePlayer),
                    Codec.DOUBLE.fieldOf("max_speed").forGetter(PotionAddDrainRecipe::maxSpeed),
                    Codec.DOUBLE.fieldOf("time").forGetter(PotionAddDrainRecipe::time)
            ).apply(x, PotionAddDrainRecipe::new)
    );

    public static PotionAddDrainRecipe of(Item item, long amount, Item result, SoundEvent sound) {
        return new PotionAddDrainRecipe(Ingredient.ofItems(item), Optional.empty(), amount, result.getDefaultStack(), Registries.SOUND_EVENT.getEntry(sound),
                false, SpoutRecipe.getMaxSpeed(FactoryFluids.POTION.defaultInstance(), amount), SpoutRecipe.getTime(FactoryFluids.POTION.defaultInstance(), amount));
    }

    @Override
    public boolean matches(DrainInput input, World world) {
        return (!requirePlayer || input.isPlayer())
                && item.test(input.stack())
                && (catalyst.isEmpty() || catalyst.get().test(input.catalyst()))
                && input.stored() + this.amount <= input.capacity();
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
        return FactoryRecipeSerializers.DRAIN_POTION_ADD;
    }

    @Override
    public List<FluidStack<?>> fluidOutput(DrainInput input) {
        var potion = input.stack().getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

        if (potion.potion().isPresent() && potion.potion().get() == Potions.WATER) {
            return List.of(FactoryFluids.WATER.defaultInstance().stackOf(this.amount));
        }

        return List.of(FactoryFluids.POTION.of(amount, potion));
    }

    @Override
    public List<FluidStack<?>> fluidInput(DrainInput input) {
        return List.of();
    }

    @Override
    public double maxSpeed(DrainInput input) {
        return this.maxSpeed;
    }

    @Override
    public double time(DrainInput input) {
        return this.time;
    }
}
