package eu.pb4.polyfactory.recipe.mixing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlockEntity;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.FireworkStarRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Collections;

public record FireworkStarMixingRecipe(double time,
                                       double minimumSpeed,
                                       double optimalSpeed) implements MixingRecipe {
    public static final FireworkStarRecipe VANILLA = new FireworkStarRecipe(CraftingRecipeCategory.BUILDING);

    public static final MapCodec<FireworkStarMixingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.DOUBLE.fieldOf("time").forGetter(FireworkStarMixingRecipe::time),
                    Codec.DOUBLE.optionalFieldOf("minimum_speed", 1d).forGetter(FireworkStarMixingRecipe::minimumSpeed),
                    Codec.DOUBLE.optionalFieldOf("optimal_speed", 1d).forGetter(FireworkStarMixingRecipe::optimalSpeed)
            ).apply(x, FireworkStarMixingRecipe::new)
    );

    @Override
    public Iterable<ItemStack> remainders() {
        return Collections.emptyList();
    }

    @Override
    public void applyRecipeUse(MixerBlockEntity inventory, World world) {
        for (int i = 0; i < MixerBlockEntity.OUTPUT_FIRST; i++) {
            var stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                stack.decrement(1);
                if (stack.isEmpty()) {
                    inventory.setStack(i, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public float minimumTemperature() {
        return 0;
    }

    @Override
    public float maxTemperature() {
        return 0.3f;
    }

    @Override
    public boolean matches(MixerBlockEntity inventory, World world) {
        return VANILLA.matches(inventory.asRecipeInputProvider(), world);
    }

    @Override
    public ItemStack craft(MixerBlockEntity inventory, RegistryWrapper.WrapperLookup registryManager) {
        return VANILLA.craft(inventory.asRecipeInputProvider(), registryManager);
    }

    @Override
    public boolean fits(int width, int height) {
        return VANILLA.fits(width, height);
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registryManager) {
        return VANILLA.getResult(registryManager);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FactoryRecipeSerializers.MIXING_FIREWORK;
    }
}
