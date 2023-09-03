package eu.pb4.polyfactory.recipe.mixing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.item.ArtificialDyeItem;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.recipe.CodecRecipeSerializer;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.minecraft.item.*;
import net.minecraft.recipe.FireworkStarRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Iterator;

public record ArtificialDyeMixingRecipe(Identifier identifier, double time,
                                        double minimumSpeed,
                                        double optimalSpeed) implements MixingRecipe {

    public static final Codec<ArtificialDyeMixingRecipe> CODEC = RecordCodecBuilder.create(x -> x.group(
                    CodecRecipeSerializer.idCodec(),
                    Codec.DOUBLE.fieldOf("time").forGetter(ArtificialDyeMixingRecipe::time),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 1d).forGetter(ArtificialDyeMixingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 1d).forGetter(ArtificialDyeMixingRecipe::optimalSpeed)
            ).apply(x, ArtificialDyeMixingRecipe::new)
    );

    @Override
    public Iterable<ItemStack> remainders() {
        return Collections.emptyList();
    }

    @Override
    public float minimumTemperature() {
        return 0.2f;
    }

    @Override
    public float maxTemperature() {
        return 1f;
    }

    @Override
    public boolean matches(MixerBlockEntity inventory, World world) {
        boolean hasBase = false;
        int dyeCount = 0;
        int ingridCount = 0;

        for (var i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getStack(i);

            // Replace with custom item
            if (stack.isOf(FactoryItems.SAW_DUST)) {
                hasBase = true;
            } else if (stack.isOf(FactoryItems.ARTIFICIAL_DYE)) {
                hasBase = true;
                dyeCount++;
                ingridCount++;
            } else if (stack.getItem() instanceof DyeItem) {
                dyeCount++;
                ingridCount++;
            } else if (stack.isOf(Items.REDSTONE) || stack.isOf(Items.SLIME_BALL) || stack.isOf(Items.LAPIS_LAZULI)
                    || stack.isOf(FactoryItems.COAL_DUST) || stack.isOf(Items.BONE_MEAL)) {
                ingridCount++;
            } else if (!stack.isEmpty()) {
                return false;
            }
        }

        return hasBase && (dyeCount > 1 || (ingridCount > 1 && dyeCount == 1));
    }

    @Override
    public ItemStack craft(MixerBlockEntity inventory, DynamicRegistryManager registryManager) {
        int[] rgb = new int[3];
        int[] rgbDye = new int[3];
        int maxColor = 0;
        int colorCount = 0;

        int delta = 32;

        for (var i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            if (inventory.getStack(i).isOf(FactoryItems.SAW_DUST)) {
                delta /= 2;
            }
        }

        for (var i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var itemStack = inventory.getStack(i);

            var color = DyeColorExtra.getColor(itemStack);
            if (color != -1) {
                var r = ColorHelper.Argb.getRed(color);
                var g = ColorHelper.Argb.getGreen(color);
                var b = ColorHelper.Argb.getBlue(color);

                rgb[0] += r;
                rgb[1] += g;
                rgb[2] += b;
                rgbDye[0] += r;
                rgbDye[1] += g;
                rgbDye[2] += b;

                maxColor += Math.max(r, Math.max(g, b));
                colorCount++;
            } else if (itemStack.isOf(Items.REDSTONE)) {
                rgb[1] = Math.max(rgb[1] - delta, 0);
                rgb[2] = Math.max(rgb[2] - delta, 0);
            } else if (itemStack.isOf(Items.SLIME_BALL)) {
                rgb[0] = Math.max(rgb[0] - delta, 0);
                rgb[2] = Math.max(rgb[2] - delta, 0);
            } else if (itemStack.isOf(Items.LAPIS_LAZULI)) {
                rgb[1] = Math.max(rgb[1] - delta, 0);
                rgb[2] = Math.max(rgb[2] - delta, 0);
            } else if (itemStack.isOf(Items.BONE_MEAL)) {
                rgb[0] += delta;
                rgb[1] += delta;
                rgb[2] += delta;
            } else if (itemStack.isOf(FactoryItems.COAL_DUST)) {
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
    public void applyRecipeUse(MixerBlockEntity inventory, World world) {
        for (int i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getStack(i);
            if (DyeColorExtra.getColor(stack) != -1
                    || stack.isOf(Items.REDSTONE) || stack.isOf(Items.SLIME_BALL) || stack.isOf(Items.LAPIS_LAZULI)
                    || stack.isOf(FactoryItems.COAL_DUST) || stack.isOf(Items.BONE_MEAL)
            ) {
                inventory.getStack(i).decrement(1);
            }
        }
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getOutput(DynamicRegistryManager registryManager) {
        return FactoryItems.ARTIFICIAL_DYE.getDefaultStack();
    }

    @Override
    public Identifier getId() {
        return this.identifier;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FactoryRecipeSerializers.MIXING_ARTIFICIAL_DYE;
    }
}
