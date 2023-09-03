package eu.pb4.polyfactory.mixin.util;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.entity.SignText;
import net.minecraft.item.DyeItem;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DyeItem.class)
public class DyeItemMixin {
    @ModifyReturnValue(method = "method_49799", at = @At("RETURN"))
    private SignText clearColor(SignText text) {
        for (int i = 0; i < 4; i++) {
            text = text.withMessage(i, text.getMessage(i, false).copy().styled(x -> x.withColor((TextColor) null)));
        }
        return text;
    }
}
