package eu.pb4.polyfactory.block.mechanical;

import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface RotationUser extends NetworkComponent {
    void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos);

    static RotationData getRotation(ServerWorld world, BlockPos pos) {
        {
            var o = FactoryNodes.ROTATIONAL.getGraphWorld(world).getNodesAt(pos).findFirst();
            if (o.isPresent()) {

                var graph = FactoryNodes.ROTATIONAL.getGraphWorld(world).getGraph(o.get().getGraphId());
                var ent = graph.getGraphEntity(RotationData.TYPE);

                ent.update(world, graph);

                return ent;
            }
        }
        return RotationData.EMPTY;
    }
}
