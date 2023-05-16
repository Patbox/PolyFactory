package eu.pb4.polyfactory.nodes;

import com.kneelawk.graphlib.GraphLib;
import com.kneelawk.graphlib.graph.BlockNodeHolder;
import com.kneelawk.graphlib.graph.NodeView;
import com.kneelawk.graphlib.graph.struct.Node;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.nodes.electric.CablePlateNode;
import eu.pb4.polyfactory.nodes.electric.DirectionalElectricalNode;
import eu.pb4.polyfactory.nodes.mechanical.*;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FactoryNodes {
    public static void register() {
        GraphLib.registerDiscoverer((world, pos) -> {
            var state = world.getBlockState(pos);
            if (state.getBlock() instanceof NetworkComponent component) {
                return component.createNodes(state, world, pos);
            }
            return List.of();
        });

        Registry.register(GraphLib.BLOCK_NODE_DECODER, ConveyorNode.ID, ConveyorNode.DECODER);
        Registry.register(GraphLib.BLOCK_NODE_DECODER, RotationalSourceNode.ID, RotationalSourceNode.DECODER);
        Registry.register(GraphLib.BLOCK_NODE_DECODER, DirectionalMechanicalNode.ID, DirectionalMechanicalNode.DECODER);
        Registry.register(GraphLib.BLOCK_NODE_DECODER, AxisMechanicalNode.ID, AxisMechanicalNode.DECODER);
        Registry.register(GraphLib.BLOCK_NODE_DECODER, GearboxNode.ID,GearboxNode.DECODER);

        Registry.register(GraphLib.BLOCK_NODE_DECODER, CablePlateNode.ID, CablePlateNode.DECODER);
        Registry.register(GraphLib.BLOCK_NODE_DECODER, DirectionalElectricalNode.ID, DirectionalElectricalNode.DECODER);
    }


    public static boolean canBothConnect(@NotNull ServerWorld world, @NotNull NodeView nodeView, @NotNull Node<BlockNodeHolder> alpha, @NotNull Node<BlockNodeHolder> beta) {
        return alpha.data().getNode().canConnect(world, nodeView, alpha.data().getPos(), alpha, beta) && beta.data().getNode().canConnect(world, nodeView, beta.data().getPos(), beta, alpha);
    }
}
