package eu.pb4.polyfactory.recipe;

import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.DyeSprayItem;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.recipe.press.PressRecipe;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import java.util.List;

public record FillSprayCanCraftingRecipe(CraftingBookCategory category) implements CraftingRecipe {
    @Override
    public boolean matches(CraftingInput inventory, Level world) {
        ItemStack can = ItemStack.EMPTY;
        int dye = -1;
        int count = 0;
        for (var stack : inventory.items()) {
            if (stack.is(FactoryItems.SPRAY_CAN)) {
                if (can.isEmpty()) {
                    can = stack;
                } else {
                    return false;
                }
            } else if (stack.is(ConventionalItemTags.DYES)) {
                count++;
                if (dye == -1) {
                    dye = DyeColorExtra.getColor(stack);
                } else if (dye != DyeColorExtra.getColor(stack)) {
                    return false;
                }
            } else if (!stack.isEmpty()) {
                return false;
            }

        }

        return !can.isEmpty() && dye != -1
                && (DyeSprayItem.getUses(can) == 0 || (DyeSprayItem.getUses(can) + 8 * count <= DyeSprayItem.MAX_USES && ColoredItem.getColor(can) == dye));
    }

    @Override
    public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registryManager) {
        ItemStack can = ItemStack.EMPTY;
        int dye = -1;
        int dyeCount = 0;
        for (var stack : inventory.items()) {
            if (stack.is(FactoryItems.SPRAY_CAN)) {
                can = stack.copy();
            } else if (stack.is(ConventionalItemTags.DYES)) {
                dyeCount++;
                if (dye == -1) {
                    dye = DyeColorExtra.getColor(stack);
                }
            }
        }

        ColoredItem.setColor(can, dye);
        DyeSprayItem.setUses(can, DyeSprayItem.getUses(can) + 8 * dyeCount);

        return can;
    }

    @Override
    public RecipeSerializer<FillSprayCanCraftingRecipe> getSerializer() {
        return FactoryRecipeSerializers.CRAFTING_FILL_SPRAY_CAN;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }
}
