package eu.pb4.polyfactory.recipe;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class PRTKeySetterCraftingRecipe extends CustomRecipe {
    public static final MapCodec<PRTKeySetterCraftingRecipe> CODEC = MapCodec.unit(() -> new PRTKeySetterCraftingRecipe(CraftingBookCategory.MISC));
    public PRTKeySetterCraftingRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput inventory, Level world) {
        int transmitter = -1;
        int key1 = -1;
        int key2 = -1;

        var x = 0;
        for (var stack : inventory.items()) {
            if (!stack.isEmpty() && !stack.is(ConventionalItemTags.DYES)) {
                if (stack.is(FactoryItems.PORTABLE_REDSTONE_TRANSMITTER)) {
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
            } else if (stack.is(ConventionalItemTags.DYES)) {
                return false;
            }
            x++;
        }

        return transmitter != -1;
    }

    @Override
    public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registryManager) {
        ItemStack transmitter = ItemStack.EMPTY;
        ItemStack key1 = ItemStack.EMPTY;
        ItemStack key2 = ItemStack.EMPTY;

        for (var stack : inventory.items()) {
            if (!stack.isEmpty()) {
                if (stack.is(FactoryItems.PORTABLE_REDSTONE_TRANSMITTER)) {
                    transmitter = stack;
                } else if (key1.isEmpty()) {
                    key1 = stack;
                } else if (key2.isEmpty()) {
                    key2 = stack;
                }
            }
        }

        var x = transmitter.copy();

        x.set(FactoryDataComponents.REMOTE_KEYS, new Pair<>(key1.copyWithCount(1), key2.copyWithCount(1)));

        return x;
    }


    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inventory) {
        NonNullList<ItemStack> defaultedList = NonNullList.withSize(inventory.size(), ItemStack.EMPTY);

        for(int i = 0; i < defaultedList.size(); ++i) {
            if (!inventory.getItem(i).is(FactoryItems.PORTABLE_REDSTONE_TRANSMITTER)) {
                defaultedList.set(i, inventory.getItem(i).copyWithCount(1));
            }
        }

        return defaultedList;
    }


    @Override
    public RecipeSerializer<PRTKeySetterCraftingRecipe> getSerializer() {
        return FactoryRecipeSerializers.CRAFTING_PRT_KEY_SETTER;
    }
}
