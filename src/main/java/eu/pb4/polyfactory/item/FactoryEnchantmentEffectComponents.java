package eu.pb4.polyfactory.item;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;

public class FactoryEnchantmentEffectComponents {
    public static final DataComponentType<EnchantmentValueEffect> CONVEYOR_PUSH_MULTIPLIER = registerValue("conveyor_push_multiplier");
    public static final DataComponentType<EnchantmentValueEffect> FAN_PUSH_MULTIPLIER = registerValue("fan_push_multiplier");

    private static DataComponentType<EnchantmentValueEffect> registerValue(String path) {
        return register(path, DataComponentType.<EnchantmentValueEffect>builder().persistent(EnchantmentValueEffect.CODEC).build());
    }
    public static void register() {
    }


    public static <T extends DataComponentType<?>> T register(String path, T item) {
        PolymerComponent.registerEnchantmentEffectComponent(item);
        return Registry.register(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(ModInit.ID, path), item);
    }
}
