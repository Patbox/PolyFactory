package eu.pb4.polyfactory.block.other;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import xyz.nucleoid.packettweaker.PacketContext;

public class InvertedRedstoneLampBlock extends RedstoneLampBlock implements PolymerBlock {
    public InvertedRedstoneLampBlock(Properties settings) {
        super(settings);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneTorchBlock.LIT, !state.getValue(RedstoneTorchBlock.LIT));
    }
}
