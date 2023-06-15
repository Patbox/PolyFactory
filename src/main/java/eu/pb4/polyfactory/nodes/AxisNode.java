package eu.pb4.polyfactory.nodes;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.nodes.mechanical.AxisMechanicalNode;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

public interface AxisNode extends BlockNode {
    static boolean canConnect(AxisMechanicalNode self, NodeHolder<BlockNode> holder, HalfLink other) {
        var tmp = other.other().getPos().subtract(holder.getPos());
        if (tmp.getSquaredDistance(Vec3i.ZERO) == 1 && tmp.getComponentAlongAxis(self.axis()) != 0) {
            if (other.other().getNode() instanceof DirectionalNode axisNode) {
                return self.axis() == axisNode.axis() && axisNode.direction().getDirection().offset() == -tmp.getComponentAlongAxis(self.axis());
            } else if (other.other().getNode() instanceof AxisNode axisNode) {
                return self.axis() == axisNode.axis();
            }

            return true;
        }

        return false;
    }

    Direction.Axis axis();
}
