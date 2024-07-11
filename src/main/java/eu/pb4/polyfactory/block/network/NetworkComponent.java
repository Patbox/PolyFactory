package eu.pb4.polyfactory.block.network;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.data.DataStorage;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.nodes.pipe.FlowData;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface NetworkComponent {
    interface Rotational extends NetworkComponent {
        static void updateRotationalAt(WorldAccess world, BlockPos pos) {
            if (world instanceof ServerWorld serverWorld) {
                FactoryNodes.ROTATIONAL.getGraphWorld(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos);


        static RotationData getLogic(ServerWorld world, BlockPos pos) {
            return RotationUser.getRotation(world, pos);
        }
    }

    interface Pipe extends NetworkComponent {
        static void updatePipeAt(WorldAccess world, BlockPos pos) {
            if (world instanceof ServerWorld serverWorld) {
                FactoryNodes.PIPE.getGraphWorld(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createPipeNodes(BlockState state, ServerWorld world, BlockPos pos);

        static FlowData getLogic(ServerWorld world, BlockPos pos) {
            var o = FactoryNodes.PIPE.getGraphWorld(world).getNodesAt(pos).findFirst();
            if (o.isPresent()) {
                return o.get().getGraph().getGraphEntity(FlowData.TYPE);
            }
            return FlowData.EMPTY;
        }

        static void forEachLogic(ServerWorld world, BlockPos pos, Consumer<FlowData> consumer) {
            FactoryNodes.PIPE.getGraphWorld(world).getNodesAt(pos).forEach(x -> {
                consumer.accept(x.getGraph().getGraphEntity(FlowData.TYPE));
            });
        }
    }

    interface RotationalConnector extends NetworkComponent {
        static void updateRotationalConnectorAt(WorldAccess world, BlockPos pos) {
            if (world instanceof ServerWorld serverWorld) {
                FactoryNodes.ROTATIONAL_CONNECTOR.getGraphWorld(serverWorld).updateNodes(pos);
            }
        }
        Collection<BlockNode> createRotationalConnectorNodes(BlockState state, ServerWorld world, BlockPos pos);
    }

    interface Data extends NetworkComponent {
        static void updateDataAt(WorldAccess world, BlockPos pos) {
            if (world instanceof ServerWorld serverWorld) {
                FactoryNodes.DATA.getGraphWorld(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos);

        static DataStorage getLogic(ServerWorld world, BlockPos pos) {
            {
                var o = FactoryNodes.DATA.getGraphWorld(world).getNodesAt(pos).findFirst();
                if (o.isPresent()) {
                    return o.get().getGraph().getGraphEntity(DataStorage.TYPE);
                }
            }
            return DataStorage.EMPTY;
        }

        static DataStorage getLogic(ServerWorld world, BlockPos pos, Predicate<NodeHolder<BlockNode>> predicate) {
            {
                var o = FactoryNodes.DATA.getGraphWorld(world).getNodesAt(pos).filter(predicate).findFirst();
                if (o.isPresent()) {
                    return o.get().getGraph().getGraphEntity(DataStorage.TYPE);
                }
            }
            return DataStorage.EMPTY;
        }
    }

    interface Energy extends NetworkComponent {
        static void updateEnergyAt(WorldAccess world, BlockPos pos) {
            if (world instanceof ServerWorld serverWorld) {
                FactoryNodes.ENERGY.getGraphWorld(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createEnergyNodes(BlockState state, ServerWorld world, BlockPos pos);
    }
}
