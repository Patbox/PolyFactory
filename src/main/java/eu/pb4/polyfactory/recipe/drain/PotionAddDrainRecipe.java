package eu.pb4.polyfactory.recipe.drain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.spout.SpoutRecipe;
import eu.pb4.polyfactory.recipe.input.DrainInput;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public record PotionAddDrainRecipe(Ingredient item, Optional<Ingredient> catalyst, long amount, ItemStack output, Holder<SoundEvent> soundEvent,
                                   boolean requirePlayer, double time) implements DrainRecipe {
    public static final MapCodec<PotionAddDrainRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Ingredient.CODEC.fieldOf("item").forGetter(PotionAddDrainRecipe::item),
                    Ingredient.CODEC.optionalFieldOf("catalyst").forGetter(PotionAddDrainRecipe::catalyst),
                    Codec.LONG.fieldOf("amount").forGetter(PotionAddDrainRecipe::amount),
                    ItemStack.SINGLE_ITEM_CODEC.fieldOf("result").forGetter(PotionAddDrainRecipe::output),
                    SoundEvent.CODEC.fieldOf("sound").forGetter(PotionAddDrainRecipe::soundEvent),
                    Codec.BOOL.optionalFieldOf("require_player", false).forGetter(PotionAddDrainRecipe::requirePlayer),
                    Codec.DOUBLE.fieldOf("time").forGetter(PotionAddDrainRecipe::time)
            ).apply(x, PotionAddDrainRecipe::new)
    );

    public static PotionAddDrainRecipe of(Item item, long amount, Item result, SoundEvent sound) {
        return new PotionAddDrainRecipe(Ingredient.of(item), Optional.empty(), amount, result.getDefaultInstance(), BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                false, SpoutRecipe.getTime(FactoryFluids.POTION.defaultInstance(), amount));
    }

    @Override
    public boolean matches(DrainInput input, Level world) {
        return (!requirePlayer || input.isPlayer())
                && item.test(input.stack())
                && (catalyst.isEmpty() || catalyst.get().test(input.catalyst()))
                && input.fluidContainer().stored() + this.amount <= input.fluidContainer().capacity();
    }

    @Override
    public ItemStack assemble(DrainInput input, HolderLookup.Provider lookup) {
        return output.copy();
    }

    @Override
    public RecipeSerializer<PotionAddDrainRecipe> getSerializer() {
        return FactoryRecipeSerializers.DRAIN_POTION_ADD;
    }

    @Override
    public List<FluidStack<?>> fluidOutput(DrainInput input) {
        var potion = input.stack().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);

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
    public double time(DrainInput input) {
        return this.time;
    }
}
