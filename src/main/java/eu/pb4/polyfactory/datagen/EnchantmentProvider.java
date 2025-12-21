package eu.pb4.polyfactory.datagen;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.item.FactoryEnchantmentEffectComponents;
import eu.pb4.polyfactory.item.FactoryEnchantments;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.SetValue;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class EnchantmentProvider extends FabricCodecDataProvider<Enchantment> {

    protected EnchantmentProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(dataOutput, registriesFuture, Registries.ENCHANTMENT, Enchantment.DIRECT_CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, Enchantment> provider, HolderLookup.Provider lookup) {
        provider.accept(FactoryEnchantments.IGNORE_MOVEMENT.identifier(), Enchantment.enchantment(
                Enchantment.definition(lookup.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.FOOT_ARMOR_ENCHANTABLE), 1 , 1,
                        Enchantment.constantCost (5), Enchantment.constantCost(45), 6, EquipmentSlotGroup.FEET))
                        .withSpecialEffect(FactoryEnchantmentEffectComponents.CONVEYOR_PUSH_MULTIPLIER, new SetValue(new LevelBasedValue.Constant(0)))
                        .withSpecialEffect(FactoryEnchantmentEffectComponents.FAN_PUSH_MULTIPLIER, new SetValue(new LevelBasedValue.Constant(0)))
                .build(FactoryEnchantments.IGNORE_MOVEMENT.identifier()));
    }

    @Override
    public String getName() {
        return "factory ench";
    }

    public static class Tags extends FabricTagProvider<Enchantment> {
        public Tags(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
            super(output, Registries.ENCHANTMENT, registriesFuture);
        }

        @Override
        public String getName() {
            return "factory ench tag";
        }

        @Override
        protected void addTags(HolderLookup.Provider wrapperLookup) {
            this.getOrCreateRawBuilder(EnchantmentTags.IN_ENCHANTING_TABLE)
                    .addOptionalElement(FactoryEnchantments.IGNORE_MOVEMENT.identifier());

            this.getOrCreateRawBuilder(EnchantmentTags.TRADEABLE)
                    .addOptionalElement(FactoryEnchantments.IGNORE_MOVEMENT.identifier());
            this.getOrCreateRawBuilder(EnchantmentTags.TRADES_DESERT_COMMON)
                    .addOptionalElement(FactoryEnchantments.IGNORE_MOVEMENT.identifier());
            this.getOrCreateRawBuilder(EnchantmentTags.ON_TRADED_EQUIPMENT)
                    .addOptionalElement(FactoryEnchantments.IGNORE_MOVEMENT.identifier());
        }
    }
}
