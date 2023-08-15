package eu.pb4.polyfactory.nodes.generic;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.CacheCategory;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface FunctionalNode extends BlockNode {
    CacheCategory<FunctionalNode> CACHE = CacheCategory.of(FunctionalNode.class);

    default Object getTargetFunctional(ServerWorld world, BlockPos pos, BlockState state) {
        return state.getBlock();
    }
}
