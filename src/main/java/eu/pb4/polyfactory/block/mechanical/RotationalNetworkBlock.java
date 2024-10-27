package eu.pb4.polyfactory.block.mechanical;

import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public abstract class RotationalNetworkBlock extends NetworkBlock implements NetworkComponent.Rotational {
    protected RotationalNetworkBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void updateNetworkAt(WorldView world, BlockPos pos) {
        NetworkComponent.Rotational.updateRotationalAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof NetworkComponent.Rotational;
    }
}