package eu.pb4.polyfactory.nodes;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.HalfLink;
import net.minecraft.util.math.Direction;

public interface DirectionNode extends AxisNode {

    static boolean canConnect(DirectionNode self, NodeHolder<BlockNode> holder, HalfLink other) {
        if (other.other().getBlockPos().offset(self.direction(), -1).equals(holder.getBlockPos())) {
            if (other.other().getNode() instanceof DirectionNode directionalNode) {
                return self.direction() == directionalNode.direction().getOpposite();
            } else if (other.other().getNode() instanceof AxisNode axisNode) {
                return self.axis() == axisNode.axis();
            }

            return true;
        }

        return false;
    }

    Direction direction();

    @Override
    default Direction.Axis axis() {
        return this.direction().getAxis();
    }
}
