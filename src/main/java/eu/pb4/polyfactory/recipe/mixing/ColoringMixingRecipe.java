package eu.pb4.polyfactory.recipe.mixing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import java.util.List;

public record ColoringMixingRecipe(String group, Item input, int maxCount, double time,
                                   double minimumSpeed,
                                   double optimalSpeed, float minimumTemperature,
                                   float maxTemperature) implements MixingRecipe {
    public static final MapCodec<ColoringMixingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(ColoringMixingRecipe::group),
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("input").forGetter(ColoringMixingRecipe::input),
                    Codec.INT.optionalFieldOf("max_count", 12).forGetter(ColoringMixingRecipe::maxCount),
                    Codec.DOUBLE.fieldOf("time").forGetter(ColoringMixingRecipe::time),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 1d).forGetter(ColoringMixingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 1d).forGetter(ColoringMixingRecipe::optimalSpeed),
                    Codec.FLOAT.optionalFieldOf("minimal_temperature", -1f).forGetter(ColoringMixingRecipe::minimumTemperature),
                    Codec.FLOAT.optionalFieldOf("max_temperature", 2f).forGetter(ColoringMixingRecipe::maxTemperature)
            ).apply(x, ColoringMixingRecipe::new)
    );


    public static RecipeHolder<ColoringMixingRecipe> of(String id, Item item, double mixingTime, double minimumSpeed, double optimalSpeed) {
        return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, FactoryUtil.id("mixing/" + id)), new ColoringMixingRecipe("", item, 12, mixingTime, minimumSpeed, optimalSpeed, -1, 2));
    }

    public static RecipeHolder<ColoringMixingRecipe> of(String id, Item item, int count, double mixingTime, double minimumSpeed, double optimalSpeed) {
        return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, FactoryUtil.id("mixing/" + id)), new ColoringMixingRecipe("", item, count, mixingTime, minimumSpeed, optimalSpeed, -1, 2));
    }


    @Override
    public boolean matches(MixingInput inventory, Level world) {
        if (!inventory.fluids().isEmpty()) {
            return false;
        }
        boolean hasDye = false;
        int count = 0;

        for (int i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getItem(i);
            if (stack.is(ConventionalItemTags.DYES)) {
                if (hasDye) {
                    return false;
                }

                hasDye = true;
            } else if (stack.is(this.input)) {
                count++;
            } else if (!stack.isEmpty()) {
                return false;
            }
        }
        return hasDye && count > 0 && count <= this.maxCount;
    }

    @Override
    public ItemStack assemble(MixingInput inventory, HolderLookup.Provider registryManager) {
        int color = -1;
        int count = 0;

        for (int i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getItem(i);
            if (stack.is(ConventionalItemTags.DYES)) {
                color = DyeColorExtra.getColor(stack);
            } else if (stack.is(this.input)) {
                count++;
            }
        }

        return ColoredItem.stackCrafting(this.input, count, color);
    }

    @Override
    public RecipeSerializer<ColoringMixingRecipe> getSerializer() {
        return FactoryRecipeSerializers.MIXING_COLORING;
    }

    @Override
    public Iterable<ItemStack> remainders(MixingInput input) {
        return List.of();
    }

    @Override
    public void applyRecipeUse(MixerBlockEntity inventory, Level world) {
        boolean hasDye = false;
        int count = this.maxCount;

        for (int i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getItem(i);
            if (stack.is(ConventionalItemTags.DYES) && !hasDye) {
                hasDye = true;
                stack.shrink(1);
            } else if (stack.is(this.input) && count != 0) {
                var removable = Math.min(count, stack.getCount());
                count -= removable;
                stack.shrink(count);
            }

            if (stack.isEmpty() && stack != ItemStack.EMPTY) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public double optimalSpeed(MixingInput input) {
        return this.optimalSpeed;
    }

    @Override
    public double minimumSpeed(MixingInput input) {
        return this.minimumSpeed;
    }

    @Override
    public float minimumTemperature(MixingInput input) {
        return this.minimumTemperature;
    }

    @Override
    public float maxTemperature(MixingInput input) {
        return this.maxTemperature;
    }

    @Override
    public double time(MixingInput input) {
        return this.time;
    }
}
