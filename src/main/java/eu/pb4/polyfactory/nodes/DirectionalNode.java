package eu.pb4.polyfactory.nodes;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.HalfLink;
import net.minecraft.util.math.Direction;

public interface DirectionalNode extends AxisNode {

    static boolean canConnect(DirectionalNode self, NodeHolder<BlockNode> holder, HalfLink other) {
        if (other.other().getBlockPos().offset(self.direction(), -1).equals(holder.getBlockPos())) {
            if (other.other().getNode() instanceof DirectionalNode directionalNode) {
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
