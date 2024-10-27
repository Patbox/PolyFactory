package eu.pb4.polyfactory.nodes;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.nodes.pipe.FlowNode;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

public interface DirectionCheckingNode extends BlockNode, FlowNode {

    boolean canConnectDir(Direction direction);

    @Override
    default boolean canFlowIn(Direction direction) {
        return canConnectDir(direction);
    }

    @Override
    default boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        var d = other.other().getBlockPos().subtract(self.getBlockPos());
        var dir = Direction.fromVector(d.getX(), d.getY(), d.getZ(), Direction.UP);

        return canConnectDir(dir);
    }
}
