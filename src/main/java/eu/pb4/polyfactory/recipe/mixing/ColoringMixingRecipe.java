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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;

import java.util.List;

public record ColoringMixingRecipe(String group, Item input, int maxCount, double time,
                                   double minimumSpeed,
                                   double optimalSpeed, float minimumTemperature,
                                   float maxTemperature) implements MixingRecipe {
    public static final MapCodec<ColoringMixingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(ColoringMixingRecipe::group),
                    Registries.ITEM.getCodec().fieldOf("input").forGetter(ColoringMixingRecipe::input),
                    Codec.INT.optionalFieldOf("max_count", 12).forGetter(ColoringMixingRecipe::maxCount),
                    Codec.DOUBLE.fieldOf("time").forGetter(ColoringMixingRecipe::time),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 1d).forGetter(ColoringMixingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 1d).forGetter(ColoringMixingRecipe::optimalSpeed),
                    Codec.FLOAT.optionalFieldOf("minimal_temperature", -1f).forGetter(ColoringMixingRecipe::minimumTemperature),
                    Codec.FLOAT.optionalFieldOf("max_temperature", 2f).forGetter(ColoringMixingRecipe::maxTemperature)
            ).apply(x, ColoringMixingRecipe::new)
    );


    public static RecipeEntry<ColoringMixingRecipe> of(String id, Item item, double mixingTime, double minimumSpeed, double optimalSpeed) {
        return new RecipeEntry<>(FactoryUtil.id("mixing/" + id), new ColoringMixingRecipe("", item, 12, mixingTime, minimumSpeed, optimalSpeed, -1, 2));
    }

    public static RecipeEntry<ColoringMixingRecipe> of(String id, Item item, int count, double mixingTime, double minimumSpeed, double optimalSpeed) {
        return new RecipeEntry<>(FactoryUtil.id("mixing/" + id), new ColoringMixingRecipe("", item, count, mixingTime, minimumSpeed, optimalSpeed, -1, 2));
    }


    @Override
    public boolean matches(MixingInput inventory, World world) {
        boolean hasDye = false;
        int count = 0;

        for (int i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getStackInSlot(i);
            if (stack.isIn(ConventionalItemTags.DYES)) {
                if (hasDye) {
                    return false;
                }

                hasDye = true;
            } else if (stack.isOf(this.input)) {
                count++;
            } else if (!stack.isEmpty()) {
                return false;
            }
        }
        return hasDye && count > 0 && count <= this.maxCount;
    }

    @Override
    public ItemStack craft(MixingInput inventory, RegistryWrapper.WrapperLookup registryManager) {
        int color = -1;
        int count = 0;

        for (int i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getStackInSlot(i);
            if (stack.isIn(ConventionalItemTags.DYES)) {
                color = DyeColorExtra.getColor(stack);
            } else if (stack.isOf(this.input)) {
                count++;
            }
        }

        return ColoredItem.stackCrafting(this.input, count, color);
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height > 1;
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registryManager) {
        return input.getDefaultStack();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FactoryRecipeSerializers.MIXING_COLORING;
    }

    @Override
    public Iterable<ItemStack> remainders() {
        return List.of();
    }

    @Override
    public void applyRecipeUse(MixerBlockEntity inventory, World world) {
        boolean hasDye = false;
        int count = this.maxCount;

        for (int i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getStack(i);
            if (stack.isIn(ConventionalItemTags.DYES) && !hasDye) {
                hasDye = true;
                stack.decrement(1);
            } else if (stack.isOf(this.input) && count != 0) {
                var removable = Math.min(count, stack.getCount());
                count -= removable;
                stack.decrement(count);
            }

            if (stack.isEmpty() && stack != ItemStack.EMPTY) {
                inventory.setStack(i, ItemStack.EMPTY);
            }
        }
    }
}
