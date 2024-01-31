package eu.pb4.polyfactory.recipe;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.item.FactoryItems;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class PRTKeySetterCraftingRecipe extends SpecialCraftingRecipe {
    public static final Codec<PRTKeySetterCraftingRecipe> CODEC = Codec.unit(() -> new PRTKeySetterCraftingRecipe(CraftingRecipeCategory.MISC));
    public PRTKeySetterCraftingRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(RecipeInputInventory inventory, World world) {
        int transmitter = -1;
        int key1 = -1;
        int key2 = -1;

        var x = 0;
        for (var stack : inventory.getHeldStacks()) {
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
    public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager registryManager) {
        ItemStack transmitter = ItemStack.EMPTY;
        ItemStack key1 = ItemStack.EMPTY;
        ItemStack key2 = ItemStack.EMPTY;

        for (var stack : inventory.getHeldStacks()) {
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

        x.getOrCreateNbt().put("key1", key1.writeNbt(new NbtCompound()));
        x.getOrCreateNbt().put("key2", key2.writeNbt(new NbtCompound()));

        return x;
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inventory) {
        DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY);

        for(int i = 0; i < defaultedList.size(); ++i) {
            if (!inventory.getStack(i).isOf(FactoryItems.PORTABLE_REDSTONE_TRANSMITTER)) {
                defaultedList.set(i, inventory.getStack(i).copyWithCount(1));
            }
        }

        return defaultedList;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FactoryRecipeSerializers.CRAFTING_PRT_KEY_SETTER;
    }
}
