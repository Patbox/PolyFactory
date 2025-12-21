package eu.pb4.polyfactory.block.network;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.data.DataStorage;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.nodes.pipe.FlowData;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface NetworkComponent {
    interface Rotational extends NetworkComponent {
        static void updateRotationalAt(LevelReader world, BlockPos pos) {
            if (world instanceof ServerLevel serverWorld) {
                FactoryNodes.ROTATIONAL.getGraphWorld(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos);


        static RotationData getLogic(ServerLevel world, BlockPos pos) {
            return RotationUser.getRotation(world, pos);
        }
    }

    interface Pipe extends NetworkComponent {
        static void updatePipeAt(LevelReader world, BlockPos pos) {
            if (world instanceof ServerLevel serverWorld) {
                FactoryNodes.PIPE.getGraphWorld(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createPipeNodes(BlockState state, ServerLevel world, BlockPos pos);

        static FlowData getLogic(ServerLevel world, BlockPos pos) {
            var o = FactoryNodes.PIPE.getGraphWorld(world).getNodesAt(pos).findFirst();
            if (o.isPresent()) {
                return o.get().getGraph().getGraphEntity(FlowData.TYPE);
            }
            return FlowData.EMPTY;
        }

        static void forEachLogic(ServerLevel world, BlockPos pos, Consumer<FlowData> consumer) {
            FactoryNodes.PIPE.getGraphWorld(world).getNodesAt(pos).forEach(x -> {
                consumer.accept(x.getGraph().getGraphEntity(FlowData.TYPE));
            });
        }
    }

    interface RotationalConnector extends NetworkComponent {
        static void updateRotationalConnectorAt(LevelReader world, BlockPos pos) {
            if (world instanceof ServerLevel serverWorld) {
                FactoryNodes.ROTATIONAL_CONNECTOR.getGraphWorld(serverWorld).updateNodes(pos);
            }
        }
        Collection<BlockNode> createRotationalConnectorNodes(BlockState state, ServerLevel world, BlockPos pos);
    }

    interface Data extends NetworkComponent {
        static void updateDataAt(LevelReader world, BlockPos pos) {
            if (world instanceof ServerLevel serverWorld) {
                FactoryNodes.DATA.getGraphWorld(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createDataNodes(BlockState state, ServerLevel world, BlockPos pos);

        static DataStorage getLogic(ServerLevel world, BlockPos pos) {
            {
                var o = FactoryNodes.DATA.getGraphWorld(world).getNodesAt(pos).findFirst();
                if (o.isPresent()) {
                    return o.get().getGraph().getGraphEntity(DataStorage.TYPE);
                }
            }
            return DataStorage.EMPTY;
        }

        static DataStorage getLogic(ServerLevel world, BlockPos pos, Predicate<NodeHolder<BlockNode>> predicate) {
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
        static void updateEnergyAt(LevelReader world, BlockPos pos) {
            if (world instanceof ServerLevel serverWorld) {
                FactoryNodes.ENERGY.getGraphWorld(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createEnergyNodes(BlockState state, ServerLevel world, BlockPos pos);
    }
}
