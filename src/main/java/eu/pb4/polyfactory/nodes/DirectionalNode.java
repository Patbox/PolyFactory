package eu.pb4.polyfactory.nodes;

import com.kneelawk.graphlib.graph.BlockNode;
import com.kneelawk.graphlib.graph.BlockNodeHolder;
import com.kneelawk.graphlib.graph.NodeView;
import com.kneelawk.graphlib.graph.struct.Node;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface DirectionalNode extends AxisNode {

    static boolean canConnect(DirectionalNode self, BlockPos pos, Node<BlockNodeHolder> other) {
        if (other.data().getPos().offset(self.direction(), -1).equals(pos)) {
            if (other.data().getNode() instanceof DirectionalNode directionalNode) {
                return self.direction() == directionalNode.direction().getOpposite();
            } else if (other.data().getNode() instanceof AxisNode axisNode) {
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
