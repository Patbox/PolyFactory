package eu.pb4.polyfactory.nodes;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.HalfLink;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;

public interface AxisNode extends BlockNode {
    static boolean canConnect(AxisNode self, NodeHolder<BlockNode> holder, HalfLink other) {
        var tmp = other.other().getBlockPos().subtract(holder.getBlockPos());
        if (tmp.getSquaredDistance(Vec3i.ZERO) == 1 && tmp.getComponentAlongAxis(self.axis()) != 0) {
            if (other.other().getNode() instanceof DirectionNode axisNode) {
                return self.axis() == axisNode.axis() && axisNode.direction().getDirection().offset() == -tmp.getComponentAlongAxis(self.axis());
            } else if (other.other().getNode() instanceof AxisNode axisNode) {
                return self.axis() == axisNode.axis();
            }

            return true;
        }

        return false;
    }

    @Override
    default boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        return AxisNode.canConnect(this, self, other);
    }

    Direction.Axis axis();
}
