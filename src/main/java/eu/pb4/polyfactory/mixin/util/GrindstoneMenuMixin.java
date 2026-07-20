package eu.pb4.polyfactory.mixin.util;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {
    @Shadow
    protected abstract ItemStack removeNonCursesFrom(ItemStack item);

    @Inject(method = "removeNonCursesFrom", at = @At("HEAD"))
    private void stripFromHead(ItemStack item, CallbackInfoReturnable<ItemStack> cir) {
        var head = item.get(FactoryDataComponents.DRILL_ATTACHMENT);
        if (head != null) {
            item.set(FactoryDataComponents.DRILL_ATTACHMENT, ItemStackTemplate.fromNonEmptyStack(this.removeNonCursesFrom(head.create())));
        }
    }
}
