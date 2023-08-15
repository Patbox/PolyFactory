package eu.pb4.polyfactory.block.electric;

import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.electric.EnergyData;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface EnergyUser extends NetworkComponent.Energy {
    void updateEnergyData(EnergyData.State modifier, BlockState state, ServerWorld world, BlockPos pos);

    static EnergyData getEnergy(ServerWorld world, BlockPos pos) {
        {
            var o = FactoryNodes.ENERGY.getGraphView(world).getNodesAt(pos).findFirst();
            if (o.isPresent()) {
                var graph = FactoryNodes.ENERGY.getGraphView(world).getGraph(o.get().getGraphId());
                var ent = graph.getGraphEntity(EnergyData.TYPE);

                ent.update(world, graph);

                return ent;
            }
        }
        return EnergyData.EMPTY;
    }
}
