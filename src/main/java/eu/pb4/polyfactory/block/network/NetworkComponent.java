package eu.pb4.polyfactory.block.network;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import java.util.Collection;

public interface NetworkComponent {
    interface Rotational extends NetworkComponent {
        static void updateRotationalAt(WorldAccess world, BlockPos pos) {
            if (world instanceof ServerWorld serverWorld) {
                //GraphLib.getController(serverWorld).updateNodes(pos);
            }
        }

        Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos);
    }
}
