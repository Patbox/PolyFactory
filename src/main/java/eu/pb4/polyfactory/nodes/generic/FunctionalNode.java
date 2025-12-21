package eu.pb4.polyfactory.nodes.generic;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.CacheCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface FunctionalNode extends BlockNode {
    CacheCategory<FunctionalNode> CACHE = CacheCategory.of(FunctionalNode.class);

    default Object getTargetFunctional(ServerLevel world, BlockPos pos, BlockState state) {
        return state.getBlock();
    }
}
