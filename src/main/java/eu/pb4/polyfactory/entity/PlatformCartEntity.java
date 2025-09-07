package eu.pb4.polyfactory.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class PlatformCartEntity extends AbstractMinecartEntity {
    protected PlatformCartEntity(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public ItemStack getPickBlockStack() {
        return null;
    }

    @Override
    protected Item asItem() {
        return null;
    }
}
