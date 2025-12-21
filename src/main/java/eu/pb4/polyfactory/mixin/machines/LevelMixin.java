package eu.pb4.polyfactory.mixin.machines;

import eu.pb4.factorytools.api.block.MultiBlock;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.output.GaugeBlock;
import eu.pb4.polyfactory.block.data.providers.DataProviderBlock;
import eu.pb4.polyfactory.block.other.FilledStateProvider;
import eu.pb4.polyfactory.data.CapacityData;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelAccessor {
    @Shadow public abstract BlockState getBlockState(BlockPos pos);

    @Shadow @Nullable public abstract BlockEntity getBlockEntity(BlockPos pos);

    @Shadow public abstract boolean setBlock(BlockPos pos, BlockState state, int flags);

    @Inject(method = "updateNeighbourForOutputSignal", at = @At("HEAD"))
    private void polyfactory$onComparatorUpdate(BlockPos pos, Block block, CallbackInfo ci) {
        //noinspection ConstantValue
        if (((Object) this) instanceof ServerLevel world) {
            this.updateItemCounter(world, pos);
        }

    }

    @Unique
    private void updateItemCounter(ServerLevel world, BlockPos pos) {
        long count = 0;
        long max = 0;

        var originalPos = pos;

        var state = world.getBlockState(pos);
        if (state.getBlock() instanceof MultiBlock multiBlock) {
            pos = multiBlock.getCenter(state, pos);
        }

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
            var selfPos = originalPos.relative(dir);
            if (this.hasChunk(SectionPos.blockToSectionCoord(selfPos.getX()), SectionPos.blockToSectionCoord(selfPos.getZ()))) {
                var selfState = this.getBlockState(selfPos);
                if (selfState.is(FactoryBlocks.ITEM_COUNTER) && selfState.getValue(DataProviderBlock.FACING) == dir.getOpposite()) {
                    DataProvider.sendData(world, selfPos, data);
                } else if (selfState.is(FactoryBlocks.GAUGE) && selfState.getValue(GaugeBlock.ORIENTATION).front() == dir) {
                    FactoryBlocks.GAUGE.receiveData(world, selfPos, selfState, -2, data, null, originalPos, null);
                }
            }
        }
    }
}
