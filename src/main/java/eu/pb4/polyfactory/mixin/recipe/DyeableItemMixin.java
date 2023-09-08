package eu.pb4.polyfactory.mixin.recipe;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import eu.pb4.polyfactory.item.ArtificialDyeItem;
import eu.pb4.polyfactory.item.ColoredItem;
import net.minecraft.item.DyeItem;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DyeableItem.class)
public interface DyeableItemMixin {
    @Inject(method = "blendAndSetColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
    private static void blendAndSetCustomColor(ItemStack stack, List<DyeItem> colors, CallbackInfoReturnable<ItemStack> cir,
                                               @Local(ordinal = 0) int[] rgb, @Local(ordinal = 0) LocalIntRef maxValue, @Local(ordinal = 1) LocalIntRef colorCount) {

        var i = maxValue.get();
        var c = 0;
        for (var itemStack : ArtificialDyeItem.CURRENT_DYES.get()) {
            var color = ColoredItem.getColor(itemStack);
            var r = ColorHelper.Argb.getRed(color);
            var g = ColorHelper.Argb.getGreen(color);
            var b = ColorHelper.Argb.getBlue(color);

            rgb[0] += r;
            rgb[1] += g;
            rgb[2] += b;

            i += Math.max(r, Math.max(g, b));
            c++;
        }
        colorCount.set(colorCount.get() + c);
        maxValue.set(i);
    }
}
