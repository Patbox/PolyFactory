package eu.pb4.polyfactory.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PlatformCartEntity extends AbstractMinecart {
    protected PlatformCartEntity(EntityType<?> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public ItemStack getPickResult() {
        return null;
    }

    @Override
    protected Item getDropItem() {
        return null;
    }
}
