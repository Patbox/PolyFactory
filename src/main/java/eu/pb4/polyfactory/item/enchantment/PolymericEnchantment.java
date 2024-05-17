package eu.pb4.polyfactory.item.enchantment;

import eu.pb4.polymer.core.api.other.PolymerEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.resource.featuretoggle.FeatureSet;

import java.util.Optional;

public class PolymericEnchantment extends Enchantment implements PolymerEnchantment {
    public PolymericEnchantment() {
        super(new Properties(ItemTags.FOOT_ARMOR_ENCHANTABLE, Optional.empty(), 4, 1, constantCost(5), constantCost(45), 6, FeatureSet.empty(), new EquipmentSlot[] { EquipmentSlot.FEET }));
    }
}
