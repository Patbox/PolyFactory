package eu.pb4.polyfactory.nodes.mechanical;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.HalfLink;
import org.jetbrains.annotations.NotNull;

public interface MechanicalNode extends BlockNode {
    @Override
    default boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        return other.other().getNode() instanceof MechanicalNode;
    };
}
