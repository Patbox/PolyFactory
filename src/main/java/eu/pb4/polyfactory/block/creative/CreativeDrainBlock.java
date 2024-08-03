package eu.pb4.polyfactory.block.creative;

import eu.pb4.polyfactory.block.fluids.DrainBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class CreativeDrainBlock extends DrainBlock {
    public CreativeDrainBlock(Settings settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeDrainBlockEntity(pos, state);
    }
}
