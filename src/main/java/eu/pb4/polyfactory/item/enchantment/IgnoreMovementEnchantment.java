package eu.pb4.polyfactory.item.enchantment;

import eu.pb4.polymer.core.api.other.PolymerEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;

public class IgnoreMovementEnchantment extends Enchantment implements PolymerEnchantment {
    public IgnoreMovementEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentTarget.ARMOR_FEET, new EquipmentSlot[] { EquipmentSlot.FEET });
    }

    @Override
    public int getMinPower(int level) {
        return 8;
    }

    @Override
    public int getMaxPower(int level) {
        return 20;
    }
}
