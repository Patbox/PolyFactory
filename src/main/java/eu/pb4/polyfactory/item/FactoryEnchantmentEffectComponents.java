package eu.pb4.polyfactory.item;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.component.ComponentType;
import net.minecraft.enchantment.effect.EnchantmentEffectEntry;
import net.minecraft.enchantment.effect.EnchantmentValueEffect;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.List;

public class FactoryEnchantmentEffectComponents {
    public static final ComponentType<EnchantmentValueEffect> CONVEYOR_PUSH_MULTIPLIER = registerValue("conveyor_push_multiplier");
    public static final ComponentType<EnchantmentValueEffect> FAN_PUSH_MULTIPLIER = registerValue("fan_push_multiplier");

    private static ComponentType<EnchantmentValueEffect> registerValue(String path) {
        return register(path, ComponentType.<EnchantmentValueEffect>builder().codec(EnchantmentValueEffect.CODEC).build());
    }
    public static void register() {
    }


    public static <T extends ComponentType<?>> T register(String path, T item) {
        PolymerComponent.registerEnchantmentEffectComponent(item);
        return Registry.register(Registries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, Identifier.of(ModInit.ID, path), item);
    }
}
