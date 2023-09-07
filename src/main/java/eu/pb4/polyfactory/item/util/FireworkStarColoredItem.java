package eu.pb4.polyfactory.item.util;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public interface FireworkStarColoredItem extends SimpleModeledPolymerItem {
    @Override
    default ItemStack getPolymerItemStack(ItemStack itemStack, TooltipContext context, @Nullable ServerPlayerEntity player) {
        var stack = SimpleModeledPolymerItem.super.getPolymerItemStack(itemStack, context, player);
        var ex = new NbtCompound();
        var c = new NbtIntArray(new int[] { getItemColor(itemStack) });
        ex.put("Colors", c);
        stack.getOrCreateNbt().put("Explosion", ex);
        return stack;
    }

    @Override
    default Item getPolymerItem() {
        return Items.FIREWORK_STAR;
    }

    int getItemColor(ItemStack stack);
}
