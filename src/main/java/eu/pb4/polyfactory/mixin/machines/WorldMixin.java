package eu.pb4.polyfactory.mixin.machines;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.output.GaugeBlock;
import eu.pb4.polyfactory.block.data.providers.DataProviderBlock;
import eu.pb4.polyfactory.block.other.FilledStateProvider;
import eu.pb4.polyfactory.data.CapacityData;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public abstract class WorldMixin implements WorldAccess {
    @Shadow public abstract BlockState getBlockState(BlockPos pos);

    @Shadow @Nullable public abstract BlockEntity getBlockEntity(BlockPos pos);

    @Shadow public abstract boolean setBlockState(BlockPos pos, BlockState state, int flags);

    @Inject(method = "updateComparators", at = @At("HEAD"))
    private void polyfactory$onComparatorUpdate(BlockPos pos, Block block, CallbackInfo ci) {
        //noinspection ConstantValue
        if (((Object) this) instanceof ServerWorld world) {
            this.updateItemCounter(world, pos);
        }

    }

    @Unique
    private void updateItemCounter(ServerWorld world, BlockPos pos) {
        long count = 0;
        long max = 0;

        if (world.getBlockEntity(pos) instanceof FilledStateProvider provider) {
            count = provider.getFilledAmount();
            max = provider.getFillCapacity();
        } else {
            var storage = ItemStorage.SIDED.find(world, pos, null);
            if (storage == null) {
                return;
            }

            for (var x : storage) {
                count += x.getAmount();
                max += x.getCapacity();
            }
        }
        var data = new CapacityData(count, max);

        for (var dir : Direction.values()) {
            var selfPos = pos.offset(dir);
            if (this.isChunkLoaded(ChunkSectionPos.getSectionCoord(selfPos.getX()), ChunkSectionPos.getSectionCoord(selfPos.getZ()))) {
                var state = this.getBlockState(selfPos);
                if (state.isOf(FactoryBlocks.ITEM_COUNTER) && state.get(DataProviderBlock.FACING) == dir.getOpposite()) {
                    DataProvider.sendData(world, selfPos, data);
                } else if (state.isOf(FactoryBlocks.GAUGE) && state.get(GaugeBlock.ORIENTATION).getFacing() == dir) {
                    FactoryBlocks.GAUGE.receiveData(world, selfPos, state, -2, data, null, pos, null);
                }
            }
        }
    }
}
