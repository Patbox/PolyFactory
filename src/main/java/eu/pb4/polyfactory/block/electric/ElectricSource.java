package eu.pb4.polyfactory.block.electric;

import eu.pb4.polyfactory.block.network.NetworkComponent;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface ElectricSource extends NetworkComponent {
    static double getNetworkSpeed(ServerWorld world, BlockPos pos) {
       /* {
            var o = GraphLib.getController(world).getNodesAt(pos).filter(x -> x.data().getNode() instanceof ElectricalNode).findFirst();
            if (o.isPresent()) {
                // Todo: Replace once there is better logic for that
                var list = GraphLib.getController(world).getGraph(o.get().data().getGraphId()).getNodes().filter(x -> x.data().getNode() instanceof ElectricalSourceNode).collect(Collectors.toList());

                if (list.size() == 0) {
                    return 0;
                }

                var speed = 0d;

                for (var entries : list) {
                    var state = world.getBlockState(entries.data().getPos());

                    if (state.getBlock() instanceof ElectricSource rotationalSource) {
                        speed += rotationalSource.getSpeed(state, world, entries.data().getPos());
                    }
                }

                return speed / list.size();
            }
        }*/
        return 0;
    }

    double getSpeed(BlockState state, ServerWorld world, BlockPos pos);
}
