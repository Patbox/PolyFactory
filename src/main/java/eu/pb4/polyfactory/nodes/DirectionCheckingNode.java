package eu.pb4.polyfactory.nodes;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.HalfLink;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

public interface DirectionCheckingNode extends BlockNode {

    boolean canConnectDir(Direction direction);

    @Override
    default boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        var d = other.other().getBlockPos().subtract(self.getBlockPos());
        var dir = Direction.fromVector(d.getX(), d.getY(), d.getZ());

        return canConnectDir(dir);
    }
}
