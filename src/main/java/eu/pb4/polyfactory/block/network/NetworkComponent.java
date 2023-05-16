package eu.pb4.polyfactory.block.network;

import com.kneelawk.graphlib.GraphLib;
import com.kneelawk.graphlib.graph.BlockNode;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;

import java.util.Collection;

public interface NetworkComponent {
    static void updateAt(WorldAccess world, BlockPos pos) {
        if (world instanceof ServerWorld serverWorld) {
            GraphLib.getController(serverWorld).updateNodes(pos);
        }
    }

    Collection<BlockNode> createNodes(BlockState state, ServerWorld world, BlockPos pos);
}
