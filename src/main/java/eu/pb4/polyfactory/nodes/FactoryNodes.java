package eu.pb4.polyfactory.nodes;


import com.kneelawk.graphlib.api.graph.GraphUniverse;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import eu.pb4.polyfactory.nodes.data.DataStorage;
import eu.pb4.polyfactory.nodes.electric.EnergyData;
import eu.pb4.polyfactory.nodes.generic.*;
import eu.pb4.polyfactory.nodes.mechanical.*;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static eu.pb4.polyfactory.ModInit.id;

public class FactoryNodes {
    public static final GraphUniverse ROTATIONAL = registerRotational();
    public static final GraphUniverse ROTATIONAL_CONNECTOR = registerRotationalConnector();
    public static final GraphUniverse ENERGY = registerEnergy();
    public static final GraphUniverse DATA = registerData();

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
        addSimpleNodes(universe);

        universe.addNodeType(ConveyorNode.TYPE);
        universe.addNodeType(AxleWithGearMechanicalNode.TYPE);
        universe.addGraphEntityType(RotationData.TYPE);

        universe.register();
        return universe;
    }

    private static GraphUniverse registerData() {
        var universe = GraphUniverse.builder().build(id("data"));
        universe.addDiscoverer((world, pos) -> {
            var blockState = world.getBlockState(pos);

            if (blockState.getBlock() instanceof NetworkComponent.Data rotational) {
                return rotational.createDataNodes(blockState, world, pos);
            }
            return List.of();
        });
        addSimpleNodes(universe);
        universe.addNodeType(ChannelProviderDirectionNode.TYPE);
        universe.addNodeType(ChannelReceiverDirectionNode.TYPE);

        universe.addGraphEntityType(DataStorage.TYPE);

        universe.register();
        return universe;
    }
    private static GraphUniverse registerEnergy() {
        var universe = GraphUniverse.builder().build(id("energy"));
        universe.addDiscoverer((world, pos) -> {
            var blockState = world.getBlockState(pos);

            if (blockState.getBlock() instanceof NetworkComponent.Energy rotational) {
                return rotational.createEnergyNodes(blockState, world, pos);
            }
            return List.of();
        });

        addSimpleNodes(universe);
        universe.addGraphEntityType(EnergyData.TYPE);
        universe.register();
        return universe;
    }

    private static GraphUniverse registerRotationalConnector() {
        var universe = GraphUniverse.builder().build(id("rotation_connector"));
        universe.addDiscoverer((world, pos) -> {
            var blockState = world.getBlockState(pos);

            if (blockState.getBlock() instanceof NetworkComponent.RotationalConnector rotational) {
                return rotational.createRotationalConnectorNodes(blockState, world, pos);
            }
            return List.of();
        });

        addSimpleNodes(universe);
        universe.register();
        return universe;
    }


    private static void addSimpleNodes(GraphUniverse universe) {
        universe.addNodeType(FunctionalDirectionNode.TYPE);
        universe.addNodeType(FunctionalAxisNode.TYPE);
        universe.addNodeType(SimpleDirectionNode.TYPE);
        universe.addNodeType(SimpleAxisNode.TYPE);
        universe.addNodeType(AllSideNode.TYPE);
        universe.addNodeType(SelectiveSideNode.TYPE);
        universe.addNodeType(NotAxisNode.TYPE);

        universe.addCacheCategory(FunctionalNode.CACHE);
    }


    public static boolean canBothConnect(@NotNull NodeHolder<BlockNode> alpha, @NotNull NodeHolder<BlockNode> beta) {
        return alpha.getNode().canConnect(alpha, new HalfLink(EmptyLinkKey.INSTANCE, beta)) && beta.getNode().canConnect(beta, new HalfLink(EmptyLinkKey.INSTANCE, alpha));
    }

    public static Stream<HalfLink> findNodes(@NotNull NodeHolder<BlockNode> self, BlockPos pos) {
        return self.getGraphWorld().getNodesAt(pos).filter(x -> FactoryNodes.canBothConnect(self, x)).map(x -> new HalfLink(EmptyLinkKey.INSTANCE, x));
    }

    public static Stream<HalfLink> findNodes(@NotNull NodeHolder<BlockNode> self, BlockPos pos, BiPredicate<NodeHolder<BlockNode>, NodeHolder<BlockNode>> predicate) {
        return self.getGraphWorld().getNodesAt(pos).filter(x -> FactoryNodes.canBothConnect(self, x) && predicate.test(self, x)).map(x -> new HalfLink(EmptyLinkKey.INSTANCE, x));
    }
}
