package eu.pb4.polyfactory.mixin.machines;

import eu.pb4.polyfactory.block.other.FilteredBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BlockEntityMixin {
    @Shadow @Nullable protected World world;

    @Inject(method = "onBlockReplaced", at = @At("TAIL"))
    private void dropFilters(BlockPos pos, BlockState oldState, CallbackInfo ci) {
        if (this.world != null && this instanceof FilteredBlockEntity blockEntity) {
            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, blockEntity.polyfactory$getFilter());
            blockEntity.polyfactory$setFilter(ItemStack.EMPTY);
        }
    }
}
