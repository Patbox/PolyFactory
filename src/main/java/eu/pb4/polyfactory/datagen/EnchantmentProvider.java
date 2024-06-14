package eu.pb4.polyfactory.datagen;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.item.FactoryEnchantmentEffectComponents;
import eu.pb4.polyfactory.item.FactoryEnchantments;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.data.DataOutput;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.effect.value.AddEnchantmentEffect;
import net.minecraft.enchantment.effect.value.SetEnchantmentEffect;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class EnchantmentProvider extends FabricCodecDataProvider<Enchantment> {

    protected EnchantmentProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(dataOutput, registriesFuture, RegistryKeys.ENCHANTMENT, Enchantment.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, Enchantment> provider, RegistryWrapper.WrapperLookup lookup) {
        provider.accept(FactoryEnchantments.IGNORE_MOVEMENT.getValue(), Enchantment.builder(
                Enchantment.definition(lookup.getWrapperOrThrow(RegistryKeys.ITEM).getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE), 1 , 1,
                        Enchantment.constantCost (5), Enchantment.constantCost(45), 6, AttributeModifierSlot.FEET))
                        .addNonListEffect(FactoryEnchantmentEffectComponents.CONVEYOR_PUSH_MULTIPLIER, new SetEnchantmentEffect(new EnchantmentLevelBasedValue.Constant(0)))
                        .addNonListEffect(FactoryEnchantmentEffectComponents.FAN_PUSH_MULTIPLIER, new SetEnchantmentEffect(new EnchantmentLevelBasedValue.Constant(0)))
                .build(FactoryEnchantments.IGNORE_MOVEMENT.getValue()));
    }

    @Override
    public String getName() {
        return "factory ench";
    }
}
