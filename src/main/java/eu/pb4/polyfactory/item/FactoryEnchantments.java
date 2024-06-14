package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.mixin.EnchantmentHelperAccessor;
import net.minecraft.component.ComponentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.effect.EnchantmentValueEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.mutable.MutableFloat;

public class FactoryEnchantments {
    public static final RegistryKey<Enchantment> IGNORE_MOVEMENT = of("ignore_movement");

    public static RegistryKey<Enchantment> of(String path) {
        return RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(ModInit.ID, path));
    }

    public static float getMultiplier(LivingEntity livingEntity, ComponentType<EnchantmentValueEffect> componentType) {
        MutableFloat mutableFloat = new MutableFloat(1);
        EnchantmentHelperAccessor.callForEachEnchantment(livingEntity, (enchantment, level, ctx) -> {
            enchantment.value().modifyValue(componentType, livingEntity.getRandom(), level, mutableFloat);
        });
        return Math.max(0, mutableFloat.intValue());
    }
}
