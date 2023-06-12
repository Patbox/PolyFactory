package eu.pb4.polyfactory.block.mechanical;

import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.mechanical.MechanicalNode;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.stream.Collectors;

public interface RotationalSource extends NetworkComponent {
    double getSpeed(BlockState state, ServerWorld world, BlockPos pos);

    static double getNetworkSpeed(ServerWorld world, BlockPos pos) {
        {
            var o = FactoryNodes.ROTATIONAL.getGraphWorld(world).getNodesAt(pos).filter(x -> x.getNode() instanceof MechanicalNode).findFirst();
            if (o.isPresent()) {
                /*
                // Todo: Replace once there is better logic for that
                var list = FactoryNodes.ROTATIONAL.getGraphWorld(world).getGraph(o.get().getGraphId())
                        .getCachedNodes();

                if (list.size() == 0) {
                    return 0;
                }

                var speed = 0d;

                for (var entries : list) {
                    var state = world.getBlockState(entries.data().getPos());

                    if (state.getBlock() instanceof RotationalSource rotationalSource) {
                        speed += rotationalSource.getSpeed(state, world, entries.data().getPos());
                    }
                }

                return speed / list.size();*/
            }
        }
        return 0.1;
        //return 0;
    }
}
