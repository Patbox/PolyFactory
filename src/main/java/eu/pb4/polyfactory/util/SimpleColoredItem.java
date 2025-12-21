package eu.pb4.polyfactory.util;

import eu.pb4.polymer.core.api.item.PolymerItem;
import it.unimi.dsi.fastutil.ints.IntList;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;

public interface SimpleColoredItem extends PolymerItem {
    @Override
    default ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context) {
        var stack = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), IntList.of(getItemColor(itemStack))));
        return stack;
    }

    @Override
    default Item getPolymerItem(ItemStack stack, PacketContext context) {
        return Items.TRIAL_KEY;
    }

    int getItemColor(ItemStack stack);
}