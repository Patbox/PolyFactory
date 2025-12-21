package eu.pb4.polyfactory.block.creative;

import eu.pb4.polyfactory.block.fluids.DrainBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CreativeDrainBlock extends DrainBlock {
    public CreativeDrainBlock(Properties settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeDrainBlockEntity(pos, state);
    }
}
