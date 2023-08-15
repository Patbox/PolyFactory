package eu.pb4.polyfactory.compat.power_networks;

import eu.pb4.polyfactory.nodes.FactoryNodes;
import io.github.mattidragon.powernetworks.PowerNetworks;
import io.github.mattidragon.powernetworks.block.CoilBlock;
import io.github.mattidragon.powernetworks.network.CoilNode;

import java.util.List;

public class PowerNetworksInit {
    public static void register() {
        FactoryNodes.ENERGY.addDiscoverer((world, pos) -> {
            var state = world.getBlockState(pos);
            if (state.getBlock() instanceof CoilBlock) {
                return List.of(new PowerNetworksNode(state.get(CoilBlock.FACING).getOpposite()));
            }
            return List.of();
        });

        FactoryNodes.ENERGY.addNodeType(PowerNetworksNode.TYPE);
    }
}
