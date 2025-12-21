package eu.pb4.polyfactory.recipe.mixing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.item.ArtificialDyeItem;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.MixingInput;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import java.util.Collections;

public record ArtificialDyeMixingRecipe(double time,
                                        double minimumSpeed,
                                        double optimalSpeed) implements MixingRecipe {

    public static final MapCodec<ArtificialDyeMixingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.DOUBLE.fieldOf("time").forGetter(artificialDyeMixingRecipe -> artificialDyeMixingRecipe.time()),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 1d).forGetter(artificialDyeMixingRecipe -> artificialDyeMixingRecipe.minimumSpeed()),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 1d).forGetter(artificialDyeMixingRecipe -> artificialDyeMixingRecipe.optimalSpeed())
            ).apply(x, ArtificialDyeMixingRecipe::new)
    );

    @Override
    public Iterable<ItemStack> remainders(MixingInput input) {
        return Collections.emptyList();
    }

    @Override
    public float minimumTemperature(MixingInput input) {
        return 0.2f;
    }

    @Override
    public float maxTemperature(MixingInput input) {
        return 1f;
    }

    @Override
    public boolean matches(MixingInput inventory, Level world) {
        if (!inventory.fluids().isEmpty()) {
            return false;
        }
        boolean hasBase = false;
        int dyeCount = 0;
        int ingridCount = 0;

        for (var i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getItem(i);

            // Replace with custom item
            if (stack.is(FactoryItems.SAW_DUST)) {
                hasBase = true;
            } else if (stack.is(FactoryItems.ARTIFICIAL_DYE)) {
                hasBase = true;
                dyeCount++;
                ingridCount++;
            } else if (stack.getItem() instanceof DyeItem) {
                dyeCount++;
                ingridCount++;
            } else if (stack.is(Items.REDSTONE) || stack.is(Items.SLIME_BALL) || stack.is(Items.LAPIS_LAZULI)
                    || stack.is(FactoryItems.COAL_DUST) || stack.is(Items.BONE_MEAL)) {
                ingridCount++;
            } else if (!stack.isEmpty()) {
                return false;
            }
        }

        return hasBase && (dyeCount > 1 || (ingridCount > 1 && dyeCount == 1));
    }

    @Override
    public ItemStack assemble(MixingInput inventory, HolderLookup.Provider registryManager) {
        int[] rgb = new int[3];
        int[] rgbDye = new int[3];
        int maxColor = 0;
        int colorCount = 0;

        int delta = 32;

        for (var i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            if (inventory.getItem(i).is(FactoryItems.SAW_DUST)) {
                delta /= 2;
            }
        }

        for (var i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var itemStack = inventory.getItem(i);

            var color = DyeColorExtra.getColor(itemStack);
            if (color != -1) {
                var r = ARGB.red(color);
                var g = ARGB.green(color);
                var b = ARGB.blue(color);

                rgb[0] += r;
                rgb[1] += g;
                rgb[2] += b;
                rgbDye[0] += r;
                rgbDye[1] += g;
                rgbDye[2] += b;

                maxColor += Math.max(r, Math.max(g, b));
                colorCount++;
            } else if (itemStack.is(Items.REDSTONE)) {
                rgb[1] = Math.max(rgb[1] - delta, 0);
                rgb[2] = Math.max(rgb[2] - delta, 0);
            } else if (itemStack.is(Items.SLIME_BALL)) {
                rgb[0] = Math.max(rgb[0] - delta, 0);
                rgb[2] = Math.max(rgb[2] - delta, 0);
            } else if (itemStack.is(Items.LAPIS_LAZULI)) {
                rgb[1] = Math.max(rgb[1] - delta, 0);
                rgb[2] = Math.max(rgb[2] - delta, 0);
            } else if (itemStack.is(Items.BONE_MEAL)) {
                rgb[0] += delta;
                rgb[1] += delta;
                rgb[2] += delta;
            } else if (itemStack.is(FactoryItems.COAL_DUST)) {
                rgb[0] = Math.max(rgb[0] - delta, 0);
                rgb[1] = Math.max(rgb[1] - delta, 0);
                rgb[2] = Math.max(rgb[2] - delta, 0);

            }
        }


        int r = rgb[0] / colorCount;
        int g = rgb[1] / colorCount;
        int b = rgb[2] / colorCount;
        float scale = (float) maxColor / (float) colorCount;
        float maxValue = (float) Math.max(rgbDye[0] / colorCount, Math.max(rgbDye[1] / colorCount, rgbDye[2] / colorCount));
        r = (int) ((float) r * scale / maxValue);
        g = (int) ((float) g * scale / maxValue);
        b = (int) ((float) b * scale / maxValue);

        int color = Math.min(r, 0xFF);
        color = (color << 8) + Math.min(g, 0xFF);
        color = (color << 8) + Math.min(b, 0xFF);

        return ArtificialDyeItem.of(color);
    }

    @Override
    public void applyRecipeUse(MixerBlockEntity inventory, Level world) {
        for (int i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getItem(i);
            if (DyeColorExtra.getColor(stack) != -1
                    || stack.is(Items.REDSTONE) || stack.is(Items.SLIME_BALL) || stack.is(Items.LAPIS_LAZULI)
                    || stack.is(FactoryItems.COAL_DUST) || stack.is(Items.BONE_MEAL)
            ) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }

        }
    }


    @Override
    public RecipeSerializer<ArtificialDyeMixingRecipe> getSerializer() {
        return FactoryRecipeSerializers.MIXING_ARTIFICIAL_DYE;
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
    public double time(MixingInput input) {
        return this.time;
    }
}
