package eu.pb4.polyfactory.mixin;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.BlockDataProviderBlock;
import eu.pb4.polyfactory.data.FactoryData;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public abstract class WorldMixin implements WorldAccess {
    @Shadow public abstract BlockState getBlockState(BlockPos pos);

    @Shadow @Nullable public abstract BlockEntity getBlockEntity(BlockPos pos);

    @SuppressWarnings("UnstableApiUsage")
    @Inject(method = "updateComparators", at = @At("HEAD"))
    private void polyfactory$onComparatorUpdate(BlockPos pos, Block block, CallbackInfo ci) {
        //noinspection ConstantValue
        if (((Object) this) instanceof ServerWorld world) {
            long count = 0;

            var storage = ItemStorage.SIDED.find(world, pos, null);
            if (storage == null) {
                return;
            }

            for (var x : storage) {
                count += x.getAmount();
            }

            for (var dir : Direction.values()) {
                var selfPos = pos.offset(dir);
                var state = this.getBlockState(selfPos);
                if (state.isOf(FactoryBlocks.BLOCK_DATA_PROVIDER) && state.get(BlockDataProviderBlock.FACING) == dir.getOpposite()) {
                    FactoryBlocks.BLOCK_DATA_PROVIDER.sendData(world, selfPos, FactoryData.of(count));
                }
            }
        }
    }
}
