package eu.pb4.polyfactory.block.data;

import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public abstract class CableNetworkBlock extends NetworkBlock implements NetworkComponent.Data, NetworkComponent.Energy{
    protected CableNetworkBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void updateNetworkAt(WorldAccess world, BlockPos pos) {
        NetworkComponent.Data.updateDataAt(world, pos);
        NetworkComponent.Energy.updateEnergyAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof NetworkComponent.Data || block instanceof NetworkComponent.Energy;
    }
}
