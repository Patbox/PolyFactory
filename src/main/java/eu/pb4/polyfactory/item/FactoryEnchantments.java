package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.mixin.EnchantmentHelperAccessor;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import org.apache.commons.lang3.mutable.MutableFloat;

public class FactoryEnchantments {
    public static final ResourceKey<Enchantment> IGNORE_MOVEMENT = of("ignore_movement");

    public static ResourceKey<Enchantment> of(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(ModInit.ID, path));
    }

    public static float getMultiplier(LivingEntity livingEntity, DataComponentType<EnchantmentValueEffect> componentType) {
        MutableFloat mutableFloat = new MutableFloat(1);
        EnchantmentHelperAccessor.callRunIterationOnEquipment(livingEntity, (enchantment, level, ctx) -> {
            enchantment.value().modifyUnfilteredValue(componentType, livingEntity.getRandom(), level, mutableFloat);
        });
        return Math.max(0, mutableFloat.intValue());
    }
}
