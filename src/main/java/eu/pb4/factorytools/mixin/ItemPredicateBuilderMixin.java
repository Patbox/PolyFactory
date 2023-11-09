package eu.pb4.factorytools.mixin;

import eu.pb4.factorytools.impl.ExtraItemPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemPredicate.Builder.class)
public class ItemPredicateBuilderMixin implements ExtraItemPredicate {
    @Unique
    private Identifier customPredicate = null;

    @Inject(method = "build", at = @At("RETURN"))
    private void buildCustom(CallbackInfoReturnable<ItemPredicate> cir) {
        if (customPredicate != null) {
            ((ExtraItemPredicate) (Object) cir.getReturnValue()).factorytools$setStaticPredicate(customPredicate);
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
