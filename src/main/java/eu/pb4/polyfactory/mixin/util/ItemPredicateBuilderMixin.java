package eu.pb4.polyfactory.mixin.util;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.advancement.ExtendedItemPredicateCodec;
import eu.pb4.polyfactory.advancement.ExtraItemPredicate;
import eu.pb4.polyfactory.advancement.FactoryItemPredicates;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemPredicate.Builder.class)
public class ItemPredicateBuilderMixin implements ExtraItemPredicate {
    @Unique
    private Identifier customPredicate = null;

    @Inject(method = "build", at = @At("RETURN"))
    private void buildCustom(CallbackInfoReturnable<ItemPredicate> cir) {
        if (customPredicate != null) {
            ((ExtraItemPredicate) (Object) cir.getReturnValue()).polyfactory$setStaticPredicate(customPredicate);
        }
    }

    @Override
    public void polyfactory$setStaticPredicate(Identifier identifier) {
        this.customPredicate = identifier;
    }

    @Override
    public Identifier polyfactory$getStaticPredicate() {
        return this.customPredicate;
    }
}
