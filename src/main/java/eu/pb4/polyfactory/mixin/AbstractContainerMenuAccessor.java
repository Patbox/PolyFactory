package eu.pb4.polyfactory.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.inventory.AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
    @Invoker
    static void callDropOrPlaceInInventory(Player player, ItemStack stack) {
        throw new UnsupportedOperationException();
    }
}
