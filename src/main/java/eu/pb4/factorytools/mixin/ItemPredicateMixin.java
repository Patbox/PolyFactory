package eu.pb4.factorytools.mixin;

import com.mojang.serialization.Codec;
import eu.pb4.factorytools.api.util.ExtraItemPredicates;
import eu.pb4.factorytools.impl.ExtendedItemPredicateCodec;
import eu.pb4.factorytools.impl.ExtraItemPredicate;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemPredicate.class)
public class ItemPredicateMixin implements ExtraItemPredicate {
    @Mutable
    @Shadow
    @Final
    public static Codec<ItemPredicate> CODEC;
    @Unique
    private Identifier customPredicate = null;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void replaceCodec(CallbackInfo ci) {
        CODEC = new ExtendedItemPredicateCodec(CODEC);
    }

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void matchCustom(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (customPredicate != null) {
            var predicate = ExtraItemPredicates.PREDICATES.get(customPredicate);
            if (predicate == null || !predicate.test(stack)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Override
    public void factorytools$setStaticPredicate(Identifier identifier) {
        this.customPredicate = identifier;
    }

    @Override
    public Identifier factorytools$getStaticPredicate() {
        return this.customPredicate;
    }
}
