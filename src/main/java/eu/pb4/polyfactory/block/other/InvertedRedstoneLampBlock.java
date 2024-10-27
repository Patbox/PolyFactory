package eu.pb4.polyfactory.block.other;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.RedstoneTorchBlock;
import xyz.nucleoid.packettweaker.PacketContext;

public class InvertedRedstoneLampBlock extends RedstoneLampBlock implements PolymerBlock {
    public InvertedRedstoneLampBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.REDSTONE_LAMP.getDefaultState().with(RedstoneTorchBlock.LIT, !state.get(RedstoneTorchBlock.LIT));
    }
}
