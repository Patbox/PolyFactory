package eu.pb4.polyfactory.mixin;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {
    @Accessor
    static TrackedData<ItemStack> getSTACK() {
        throw new UnsupportedOperationException();
    }
}
