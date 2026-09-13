package eu.pb4.polyfactory.mixin.util;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DyeItem.class)
public class DyeItemMixin {
    @ModifyReturnValue(method = "lambda$tryApplyToSign$0", at = @At("RETURN"))
    private static SignText clearColor(SignText text) {
        var mut = text.asMutable();
        for (int i = 0; i < 4; i++) {
            mut = mut.setLine(i, text.getMessages(false).get(i).copy().setStyle(text.getMessages(false).get(i).getStyle().withColor((TextColor) null)));
        }
        return mut.asImmutable();
    }
}
