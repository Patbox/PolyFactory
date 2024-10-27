package eu.pb4.polyfactory.recipe;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class PRTKeySetterCraftingRecipe extends SpecialCraftingRecipe {
    public static final MapCodec<PRTKeySetterCraftingRecipe> CODEC = MapCodec.unit(() -> new PRTKeySetterCraftingRecipe(CraftingRecipeCategory.MISC));
    public PRTKeySetterCraftingRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput inventory, World world) {
        int transmitter = -1;
        int key1 = -1;
        int key2 = -1;

        var x = 0;
        for (var stack : inventory.getStacks()) {
            if (!stack.isEmpty() && !stack.isIn(ConventionalItemTags.DYES)) {
                if (stack.isOf(FactoryItems.PORTABLE_REDSTONE_TRANSMITTER)) {
                    if (transmitter != -1) {
                        return false;
                    } else {
                        transmitter = x;
                    }
                } else if (key1 == -1) {
                    key1 = x;
                } else if (key2 == -1) {
                    key2 = x;
                } else {
                    return false;
                }
            } else if (stack.isIn(ConventionalItemTags.DYES)) {
                return false;
            }
            x++;
        }

        return transmitter != -1;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput inventory, RegistryWrapper.WrapperLookup registryManager) {
        ItemStack transmitter = ItemStack.EMPTY;
        ItemStack key1 = ItemStack.EMPTY;
        ItemStack key2 = ItemStack.EMPTY;

        for (var stack : inventory.getStacks()) {
            if (!stack.isEmpty()) {
                if (stack.isOf(FactoryItems.PORTABLE_REDSTONE_TRANSMITTER)) {
                    transmitter = stack;
                } else if (key1.isEmpty()) {
                    key1 = stack;
                } else if (key2.isEmpty()) {
                    key2 = stack;
                }
            }
        }

        var x = transmitter.copy();

        x.set(FactoryDataComponents.REMOTE_KEYS, new Pair<>(key1, key2));

        return x;
    }


    @Override
    public DefaultedList<ItemStack> getRecipeRemainders(CraftingRecipeInput inventory) {
        DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY);

        for(int i = 0; i < defaultedList.size(); ++i) {
            if (!inventory.getStackInSlot(i).isOf(FactoryItems.PORTABLE_REDSTONE_TRANSMITTER)) {
                defaultedList.set(i, inventory.getStackInSlot(i).copyWithCount(1));
            }
        }

        return defaultedList;
    }


    @Override
    public RecipeSerializer<PRTKeySetterCraftingRecipe> getSerializer() {
        return FactoryRecipeSerializers.CRAFTING_PRT_KEY_SETTER;
    }
}
