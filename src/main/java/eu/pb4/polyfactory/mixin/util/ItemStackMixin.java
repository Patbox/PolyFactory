package eu.pb4.polyfactory.mixin.util;

import eu.pb4.polyfactory.item.tool.DrillItem;
import eu.pb4.polyfactory.item.util.CustomItemBrokenHandler;
import net.fabricmc.fabric.api.item.v1.CustomDamageHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Shadow
    public abstract Item getItem();

    @Inject(method = "applyDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"), cancellable = true)
    private void proxyDamage(int newDamage, @Nullable ServerPlayer player, Consumer<Item> onBreak, CallbackInfo ci) {
        if (this.getItem() instanceof CustomItemBrokenHandler handler && handler.onItemBreakingDamageApplied((ItemStack) (Object) this, player, onBreak)) {
            ci.cancel();
        }
    }
}
