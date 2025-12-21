package eu.pb4.polyfactory.block.mechanical;

import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

public abstract class RotationalNetworkBlock extends NetworkBlock implements NetworkComponent.Rotational {
    protected RotationalNetworkBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void updateNetworkAt(LevelReader world, BlockPos pos) {
        NetworkComponent.Rotational.updateRotationalAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof NetworkComponent.Rotational;
    }
}