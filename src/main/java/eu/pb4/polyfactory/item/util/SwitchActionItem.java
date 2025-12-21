package eu.pb4.polyfactory.item.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public interface SwitchActionItem {
    boolean onSwitchAction(ServerPlayer player, ItemStack main, InteractionHand mainHand);
}
