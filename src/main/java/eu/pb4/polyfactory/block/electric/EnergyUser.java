package eu.pb4.polyfactory.block.electric;

import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.electric.EnergyData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface EnergyUser extends NetworkComponent.Energy {
    void updateEnergyData(EnergyData.State modifier, BlockState state, ServerLevel world, BlockPos pos);

    static EnergyData getEnergy(ServerLevel world, BlockPos pos) {
        {
            var o = FactoryNodes.ENERGY.getGraphWorld(world).getNodesAt(pos).findFirst();
            if (o.isPresent()) {
                var graph = FactoryNodes.ENERGY.getGraphWorld(world).getGraph(o.get().getGraphId());
                var ent = graph.getGraphEntity(EnergyData.TYPE);

                ent.update(world, graph);

                return ent;
            }
        }
        return EnergyData.EMPTY;
    }
}
