package eu.pb4.polyfactory.item.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public interface CustomItemBrokenHandler {
    boolean onItemBreakingDamageApplied(ItemStack itemStack, @Nullable ServerPlayer player, Consumer<Item> onBreak);
}
