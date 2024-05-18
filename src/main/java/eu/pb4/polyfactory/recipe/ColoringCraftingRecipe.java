package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public record ColoringCraftingRecipe(String group, Item input, int maxCount) implements CraftingRecipe {
    public static final MapCodec<ColoringCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(ColoringCraftingRecipe::group),
                    Registries.ITEM.getCodec().fieldOf("input").forGetter(ColoringCraftingRecipe::input),
                    Codec.INT.optionalFieldOf("max_count", 8).forGetter(ColoringCraftingRecipe::maxCount)
            ).apply(x, ColoringCraftingRecipe::new)
    );


    public static RecipeEntry<ColoringCraftingRecipe> of(String id, Item item) {
        return new RecipeEntry<>(FactoryUtil.id("crafting/" + id), new ColoringCraftingRecipe("", item, 8));
    }

    public static RecipeEntry<ColoringCraftingRecipe> of(String id, Item item, int count) {
        return new RecipeEntry<>(FactoryUtil.id("crafting/" + id), new ColoringCraftingRecipe("", item, count));
    }

    @Override
    public CraftingRecipeCategory getCategory() {
        return CraftingRecipeCategory.MISC;
    }

    @Override
    public boolean matches(RecipeInputInventory inventory, World world) {
        boolean hasDye = false;
        int count = 0;
        for (var stack : inventory.getHeldStacks()) {
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
    public ItemStack craft(RecipeInputInventory inventory, RegistryWrapper.WrapperLookup registryManager) {
        int color = -1;
        int count = 0;
        ItemStack og = ItemStack.EMPTY;

        for (var stack : inventory.getHeldStacks()) {
            if (stack.isIn(ConventionalItemTags.DYES)) {
                color = DyeColorExtra.getColor(stack);
            } else if (stack.isOf(this.input)) {
                count++;
                og = stack;
            }
        }

        var out = ColoredItem.stackCrafting(this.input, count, color);
        if (this.maxCount == 1) {
            var c = out.get(FactoryDataComponents.COLOR);
            out.applyChanges(og.getComponentChanges());
            out.set(FactoryDataComponents.COLOR, c);
        }
        return out;
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
        return FactoryRecipeSerializers.CRAFTING_COLORING;
    }
}
