package eu.pb4.polyfactory.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
    @Accessor
    static EntityDataAccessor<ItemStack> getDATA_ITEM() {
        throw new UnsupportedOperationException();
    }
}
