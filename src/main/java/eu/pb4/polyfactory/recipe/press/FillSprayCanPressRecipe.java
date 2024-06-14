package eu.pb4.polyfactory.recipe.press;

import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlockEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.DyeSprayItem;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.PressInput;
import eu.pb4.polyfactory.util.DyeColorExtra;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.RegistryWrapper;
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
    public boolean matches(PressInput inventory, World world) {
        var can = inventory.input();
        var dye = inventory.pattern();
        return can.isOf(FactoryItems.SPRAY_CAN) && dye.isIn(ConventionalItemTags.DYES)
                && (DyeSprayItem.getUses(can) == 0 || (DyeSprayItem.getUses(can) + amount <= DyeSprayItem.MAX_USES && ColoredItem.getColor(can) == DyeColorExtra.getColor(dye)));
    }

    @Override
    public ItemStack craft(PressInput inventory, RegistryWrapper.WrapperLookup registryManager) {
        var can = inventory.input().copy();
        var dye = inventory.pattern();
        ColoredItem.setColor(can, DyeColorExtra.getColor(dye));
        DyeSprayItem.setUses(can, DyeSprayItem.getUses(can) + amount);
        return can;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup registryManager) {
        return FactoryItems.SPRAY_CAN.getDefaultStack();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FactoryRecipeSerializers.PRESS_FILL_SPRAY_CAN;
    }
}
