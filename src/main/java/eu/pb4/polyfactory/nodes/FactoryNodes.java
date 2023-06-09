package eu.pb4.polyfactory.nodes;


import com.kneelawk.graphlib.api.graph.GraphUniverse;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.nodes.mechanical.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static eu.pb4.polyfactory.ModInit.id;

public class FactoryNodes {
    public static final GraphUniverse ROTATIONAL = registerRotational();

    public static void register() {
    }

    private static GraphUniverse registerRotational() {
        var universe = GraphUniverse.builder().build(id("rotations"));


        universe.addDiscoverer((world, pos) -> {
            var blockState = world.getBlockState(pos);

            if (blockState.getBlock() instanceof NetworkComponent.Rotational rotational) {
                return rotational.createRotationalNodes(blockState, world, pos);
            }
            return List.of();
        });

        universe.addNodeType(ConveyorNode.TYPE);
        universe.addNodeType(DirectionalRotationUserNode.TYPE);
        universe.addNodeType(DirectionalMechanicalNode.TYPE);
        universe.addNodeType(AxisRotationUserNode.TYPE);
        universe.addNodeType(AxisMechanicalNode.TYPE);
        universe.addNodeType(AllSideNode.TYPE);

        universe.addCacheCategory(RotationUserNode.CACHE);

        universe.addGraphEntityType(RotationData.TYPE);

        //universe.addNodeType(CablePlateNode.TYPE, CablePlateNode.DECODER);
        //universe.addNodeType(DirectionalElectricalNode.TYPE, DirectionalElectricalNode.DECODER);

        universe.register();
        return universe;
    }


    public static boolean canBothConnect(@NotNull NodeHolder<BlockNode> alpha, @NotNull NodeHolder<BlockNode> beta) {
        return alpha.getNode().canConnect(alpha, new HalfLink(EmptyLinkKey.INSTANCE, beta)) && beta.getNode().canConnect(beta, new HalfLink(EmptyLinkKey.INSTANCE, alpha));
    }
}
