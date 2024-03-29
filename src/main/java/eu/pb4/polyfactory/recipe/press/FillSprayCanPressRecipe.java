package eu.pb4.polyfactory.recipe.press;

import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.DyeSprayItem;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.world.World;

public record FillSprayCanPressRecipe(int amount) implements PressRecipe {
    @Override
    public double minimumSpeed() {
        return 1;
    }

    @Override
    public void applyRecipeUse(PressBlockEntity inventory, World world) {
        inventory.setStack(0, ItemStack.EMPTY);
        inventory.getStack(1).decrement(1);
    }

    @Override
    public boolean matches(PressBlockEntity inventory, World world) {
        var can = inventory.getStack(0);
        var dye = inventory.getStack(1);
        return can.isOf(FactoryItems.SPRAY_CAN) && dye.isIn(ConventionalItemTags.DYES)
                && (DyeSprayItem.getUses(can) == 0 || (DyeSprayItem.getUses(can) + amount <= DyeSprayItem.MAX_USES && ColoredItem.getColor(can) == DyeColorExtra.getColor(dye)));
    }

    @Override
    public ItemStack craft(PressBlockEntity inventory, DynamicRegistryManager registryManager) {
        var can = inventory.getStack(0).copy();
        var dye = inventory.getStack(1);
        ColoredItem.setColor(can, DyeColorExtra.getColor(dye));
        DyeSprayItem.setUses(can, DyeSprayItem.getUses(can) + amount);
        return can;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResult(DynamicRegistryManager registryManager) {
        return FactoryItems.SPRAY_CAN.getDefaultStack();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FactoryRecipeSerializers.PRESS_FILL_SPRAY_CAN;
    }
}
