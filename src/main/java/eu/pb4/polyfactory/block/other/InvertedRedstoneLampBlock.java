package eu.pb4.polyfactory.block.other;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.*;

public class InvertedRedstoneLampBlock extends RedstoneLampBlock implements PolymerBlock {
    public InvertedRedstoneLampBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.REDSTONE_LAMP.getDefaultState().with(RedstoneTorchBlock.LIT, !state.get(RedstoneTorchBlock.LIT));
    }
}
