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

import java.util.Optional;

@Mixin(ItemPredicate.class)
public class ItemPredicateMixin implements ExtraItemPredicate {
    @Mutable
    @Shadow @Final public static Codec<ItemPredicate> CODEC;
    @Unique
    private Identifier customPredicate = null;

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void matchCustom(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (customPredicate != null) {
            var predicate = FactoryItemPredicates.PREDICATES.get(customPredicate);
            if (predicate == null || !predicate.test(stack)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void replaceCodec(CallbackInfo ci) {
        CODEC = new ExtendedItemPredicateCodec(CODEC);
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
