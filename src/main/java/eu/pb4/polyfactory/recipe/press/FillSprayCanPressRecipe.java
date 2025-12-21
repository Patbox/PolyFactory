package eu.pb4.polyfactory.recipe.press;

import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.DyeSprayItem;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.PressInput;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public record FillSprayCanPressRecipe(int amount) implements PressRecipe {
    @Override
    public double minimumSpeed() {
        return 1;
    }

    @Override
    public void applyRecipeUse(PressBlockEntity inventory, Level world) {
        inventory.setItem(0, ItemStack.EMPTY);
        inventory.getItem(1).shrink(1);
    }

    @Override
    public boolean matches(PressInput inventory, Level world) {
        var can = inventory.input();
        var dye = inventory.pattern();
        return can.is(FactoryItems.SPRAY_CAN) && dye.is(ConventionalItemTags.DYES)
                && (DyeSprayItem.getUses(can) == 0 || (DyeSprayItem.getUses(can) + amount <= DyeSprayItem.MAX_USES && ColoredItem.getColor(can) == DyeColorExtra.getColor(dye)));
    }

    @Override
    public ItemStack assemble(PressInput inventory, HolderLookup.Provider registryManager) {
        var can = inventory.input().copy();
        var dye = inventory.pattern();
        ColoredItem.setColor(can, DyeColorExtra.getColor(dye));
        DyeSprayItem.setUses(can, DyeSprayItem.getUses(can) + amount);
        return can;
    }

    @Override
    public RecipeSerializer<FillSprayCanPressRecipe> getSerializer() {
        return FactoryRecipeSerializers.PRESS_FILL_SPRAY_CAN;
    }
}
