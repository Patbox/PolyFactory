package eu.pb4.polyfactory.block;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public interface BarrierBasedWaterloggable extends QuickWaterloggable, PolymerBlock {
    @Override
    default Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    default BlockState getPolymerBlockState(BlockState state) {
        return Blocks.BARRIER.getDefaultState().with(WATERLOGGED, state.get(WATERLOGGED));
    }
}
