package eu.pb4.polyfactory.datagen;

import eu.pb4.polyfactory.item.FactoryEnchantmentEffectComponents;
import eu.pb4.polyfactory.item.FactoryEnchantments;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentLevelBasedValue;
import net.minecraft.enchantment.effect.value.SetEnchantmentEffect;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class EnchantmentTagProvider extends FabricTagProvider<Enchantment> {


    public EnchantmentTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, RegistryKeys.ENCHANTMENT, registriesFuture);
    }

    @Override
    public String getName() {
        return "factory ench tag";
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        this.getOrCreateTagBuilder(EnchantmentTags.IN_ENCHANTING_TABLE)
                .addOptional(FactoryEnchantments.IGNORE_MOVEMENT);

        this.getOrCreateTagBuilder(EnchantmentTags.TRADEABLE)
                .addOptional(FactoryEnchantments.IGNORE_MOVEMENT);
        this.getOrCreateTagBuilder(EnchantmentTags.DESERT_COMMON_TRADE)
                .addOptional(FactoryEnchantments.IGNORE_MOVEMENT);
        this.getOrCreateTagBuilder(EnchantmentTags.ON_TRADED_EQUIPMENT)
                .addOptional(FactoryEnchantments.IGNORE_MOVEMENT);
    }
}
