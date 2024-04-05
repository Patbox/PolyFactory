package eu.pb4.polyfactory.item.util;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

public interface SwitchActionItem {
    boolean onSwitchAction(ServerPlayerEntity player, ItemStack main, Hand mainHand);
}
