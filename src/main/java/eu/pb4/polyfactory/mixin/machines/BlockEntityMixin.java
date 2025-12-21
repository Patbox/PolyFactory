package eu.pb4.polyfactory.mixin.machines;

import eu.pb4.polyfactory.block.other.FilteredBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BlockEntityMixin {
    @Shadow @Nullable protected Level level;

    @Inject(method = "preRemoveSideEffects", at = @At("TAIL"))
    private void dropFilters(BlockPos pos, BlockState oldState, CallbackInfo ci) {
        if (this.level != null && this instanceof FilteredBlockEntity blockEntity) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, blockEntity.polyfactory$getFilter());
            blockEntity.polyfactory$setFilter(ItemStack.EMPTY);
        }
    }
}
