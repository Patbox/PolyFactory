package eu.pb4.polyfactory.mixin.recipe;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import eu.pb4.polyfactory.item.ArtificialDyeItem;
import eu.pb4.polyfactory.item.util.ColoredItem;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.ColorHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DyedColorComponent.class)
public class DyedColorComponentMixin {
    @Inject(method = "setColor", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private static void blendAndSetCustomColor(ItemStack stack, List<DyeItem> colors, CallbackInfoReturnable<ItemStack> cir,
                                               @Local(ordinal = 0) LocalIntRef rx,
                                               @Local(ordinal = 1) LocalIntRef gx,
                                               @Local(ordinal = 2) LocalIntRef bx,
                                               @Local(ordinal = 3) LocalIntRef maxValue, @Local(ordinal = 4) LocalIntRef colorCount) {

        var i = maxValue.get();
        var c = 0;
        int r = rx.get(), g = gx.get(), b = bx.get();
        for (var itemStack : ArtificialDyeItem.CURRENT_DYES.get()) {
            var color = ColoredItem.getColor(itemStack);
            r += ColorHelper.Argb.getRed(color);
            g += ColorHelper.Argb.getGreen(color);
            b += ColorHelper.Argb.getBlue(color);
            i += Math.max(r, Math.max(g, b));
            c++;
        }
        colorCount.set(colorCount.get() + c);
        maxValue.set(i);
        rx.set(r);
        gx.set(g);
        bx.set(b);
    }
}
