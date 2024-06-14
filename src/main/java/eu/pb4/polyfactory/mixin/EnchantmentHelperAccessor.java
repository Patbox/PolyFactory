package eu.pb4.polyfactory.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EnchantmentHelper.class)
public interface EnchantmentHelperAccessor {
    @Invoker
    static void callForEachEnchantment(LivingEntity entity, EnchantmentHelper.ContextAwareConsumer contextAwareConsumer) {
        throw new UnsupportedOperationException();
    }
}
