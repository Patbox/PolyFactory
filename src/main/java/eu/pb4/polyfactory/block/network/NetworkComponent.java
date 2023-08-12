package eu.pb4.polyfactory.block.network;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import java.util.Collection;

public interface NetworkComponent {
    interface Rotational extends NetworkComponent {
        static void updateRotationalAt(WorldAccess world, BlockPos pos) {
            if (world instanceof ServerWorld serverWorld) {
                FactoryNodes.ROTATIONAL.getServerGraphWorld(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos);
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
