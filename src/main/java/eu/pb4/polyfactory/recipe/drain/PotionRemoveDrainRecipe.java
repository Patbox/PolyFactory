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

public record PotionRemoveDrainRecipe(Ingredient item, Optional<Ingredient> catalyst, long amount, ItemStack output, Holder<SoundEvent> soundEvent,
                                      boolean requirePlayer, double time) implements DrainRecipe {
    public static final MapCodec<PotionRemoveDrainRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Ingredient.CODEC.fieldOf("item").forGetter(PotionRemoveDrainRecipe::item),
                    Ingredient.CODEC.optionalFieldOf("catalyst").forGetter(PotionRemoveDrainRecipe::catalyst),
                    Codec.LONG.fieldOf("amount").forGetter(PotionRemoveDrainRecipe::amount),
                    ItemStack.SINGLE_ITEM_CODEC.fieldOf("result").forGetter(PotionRemoveDrainRecipe::output),
                    SoundEvent.CODEC.fieldOf("sound").forGetter(PotionRemoveDrainRecipe::soundEvent),
                    Codec.BOOL.optionalFieldOf("require_player", false).forGetter(PotionRemoveDrainRecipe::requirePlayer),
                    Codec.DOUBLE.fieldOf("time").forGetter(PotionRemoveDrainRecipe::time)
            ).apply(x, PotionRemoveDrainRecipe::new)
    );

    public static PotionRemoveDrainRecipe of(Item item, long amount, Item result, SoundEvent sound) {
        return new PotionRemoveDrainRecipe(Ingredient.of(item), Optional.empty(), amount, result.getDefaultInstance(), BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                true, SpoutRecipe.getTime(FactoryFluids.POTION.defaultInstance(), amount));
    }

    @Override
    public boolean matches(DrainInput input, Level world) {
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
    public ItemStack assemble(DrainInput input, HolderLookup.Provider lookup) {
        for (var key : input.fluids()) {
            if ((key.type() == FactoryFluids.POTION || key.type() == FactoryFluids.WATER) && input.getFluid(key) >= amount) {
                var stack = this.output.copy();
                stack.set(DataComponents.POTION_CONTENTS, key.type() == FactoryFluids.POTION
                        ? (PotionContents) key.data()
                        : PotionContents.EMPTY.withPotion(Potions.WATER));
                return stack;
            }
        }

        return output.copy();
    }

    @Override
    public RecipeSerializer<PotionRemoveDrainRecipe> getSerializer() {
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
