package eu.pb4.polyfactory.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EnchantmentHelper.class)
public interface EnchantmentHelperAccessor {
    @Invoker
    static void callRunIterationOnEquipment(LivingEntity entity, EnchantmentHelper.EnchantmentInSlotVisitor contextAwareConsumer) {
        throw new UnsupportedOperationException();
    }
}
