package eu.pb4.polyfactory.block.data;

import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

public abstract class CableNetworkBlock extends NetworkBlock implements NetworkComponent.Data, NetworkComponent.Energy{
    protected CableNetworkBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void updateNetworkAt(LevelReader world, BlockPos pos) {
        NetworkComponent.Data.updateDataAt(world, pos);
        NetworkComponent.Energy.updateEnergyAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof NetworkComponent.Data || block instanceof NetworkComponent.Energy;
    }
}
