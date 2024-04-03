package eu.pb4.polyfactory.block.network;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.data.DataStorage;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

import java.util.Collection;
import java.util.function.Predicate;

public interface NetworkComponent {
    interface Rotational extends NetworkComponent {
        static void updateRotationalAt(WorldAccess world, BlockPos pos) {
            if (world instanceof ServerWorld serverWorld) {
                FactoryNodes.ROTATIONAL.getServerGraphWorld(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos);
    }

    interface RotationalConnector extends NetworkComponent {
        static void updateRotationalConnectorAt(WorldAccess world, BlockPos pos) {
            if (world instanceof ServerWorld serverWorld) {
                FactoryNodes.ROTATIONAL_CONNECTOR.getServerGraphWorld(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createRotationalConnectorNodes(BlockState state, ServerWorld world, BlockPos pos);
    }

    interface Data extends NetworkComponent {
        static void updateDataAt(WorldAccess world, BlockPos pos) {
            if (world instanceof ServerWorld serverWorld) {
                FactoryNodes.DATA.getServerGraphWorld(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos);

        static DataStorage getLogic(ServerWorld world, BlockPos pos) {
            {
                var o = FactoryNodes.DATA.getGraphView(world).getNodesAt(pos).findFirst();
                if (o.isPresent()) {
                    var graph = FactoryNodes.DATA.getGraphView(world).getGraph(o.get().getGraphId());
                    var ent = graph.getGraphEntity(DataStorage.TYPE);

                    return ent;
                }
            }
            return DataStorage.EMPTY;
        }

        static DataStorage getLogic(ServerWorld world, BlockPos pos, Predicate<NodeHolder<BlockNode>> predicate) {
            {
                var o = FactoryNodes.DATA.getGraphView(world).getNodesAt(pos).filter(predicate).findFirst();
                if (o.isPresent()) {
                    var graph = FactoryNodes.DATA.getGraphView(world).getGraph(o.get().getGraphId());
                    var ent = graph.getGraphEntity(DataStorage.TYPE);

                    return ent;
                }
            }
            return DataStorage.EMPTY;
        }
    }

    interface Energy extends NetworkComponent {
        static void updateEnergyAt(WorldAccess world, BlockPos pos) {
            if (world instanceof ServerWorld serverWorld) {
                FactoryNodes.ENERGY.getServerGraphWorld(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createEnergyNodes(BlockState state, ServerWorld world, BlockPos pos);
    }
}
