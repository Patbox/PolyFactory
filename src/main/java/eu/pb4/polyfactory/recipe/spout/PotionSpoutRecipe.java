package eu.pb4.polyfactory.recipe.spout;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.SingleItemWithFluid;
import java.util.List;
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

public record PotionSpoutRecipe(Ingredient item, long amount, ItemStack output, Holder<SoundEvent> soundEvent, double time) implements SpoutRecipe {
    public static final MapCodec<PotionSpoutRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Ingredient.CODEC.fieldOf("item").forGetter(PotionSpoutRecipe::item),
                    Codec.LONG.fieldOf("amount").forGetter(PotionSpoutRecipe::amount),
                    ItemStack.SINGLE_ITEM_CODEC.fieldOf("result").forGetter(PotionSpoutRecipe::output),
                    SoundEvent.CODEC.fieldOf("sound").forGetter(PotionSpoutRecipe::soundEvent),
                    Codec.DOUBLE.fieldOf("time").forGetter(PotionSpoutRecipe::time)
            ).apply(x, PotionSpoutRecipe::new)
    );

    public static PotionSpoutRecipe of(Item item, long amount, Item result, SoundEvent sound) {
        return new PotionSpoutRecipe(Ingredient.of(item), amount, result.getDefaultInstance(), BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                SpoutRecipe.getTime(FactoryFluids.POTION.defaultInstance(), amount));
    }

    @Override
    public boolean matches(SingleItemWithFluid input, Level world) {
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
    public ItemStack assemble(SingleItemWithFluid input, HolderLookup.Provider lookup) {
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
    public RecipeSerializer<PotionSpoutRecipe> getSerializer() {
        return FactoryRecipeSerializers.SPOUT_POTION;
    }

    @Override
    public List<FluidStack<?>> fluidInput(SingleItemWithFluid input) {
        for (var key : input.fluids()) {
            if ((key.type() == FactoryFluids.POTION || key.type() == FactoryFluids.WATER) && input.getFluid(key) >= amount) {
                return List.of(key.stackOf(this.amount));
            }
        }
        return List.of();
    }

    @Override
    public double time(SingleItemWithFluid input) {
        return this.time;
    }
}
