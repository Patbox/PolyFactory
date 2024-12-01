package eu.pb4.polyfactory.util;

import eu.pb4.polymer.core.api.item.PolymerItem;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public interface SimpleColoredItem extends PolymerItem {
    @Override
    default ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        var stack = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);
        stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(), List.of(), List.of(), IntList.of(getItemColor(itemStack))));
        return stack;
    }

    @Override
    default Item getPolymerItem(ItemStack stack, PacketContext context) {
        return Items.TRIAL_KEY;
    }

    int getItemColor(ItemStack stack);
}