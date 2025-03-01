package eu.pb4.polyfactory.mixin.block;

import eu.pb4.polyfactory.util.inventory.CrafterLikeInsertInventory;
import net.minecraft.block.entity.CrafterBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CrafterBlockEntity.class)
public abstract class CrafterBlockEntityMixin implements CrafterLikeInsertInventory {
    @Shadow public abstract boolean isSlotDisabled(int slot);

    @Override
    public boolean isSlotLocked(int i) {
        return this.isSlotDisabled(i);
    }

    @Override
    public int inputSize() {
        return 9;
    }
}
