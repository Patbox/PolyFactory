package eu.pb4.polyfactory.nodes;

import net.minecraft.util.math.Direction;

public interface AxisNode {
    Direction.Axis axis();

    /*static boolean canConnect(AxisNode self, BlockPos pos, Node<BlockNodeHolder> other) {
        var tmp = other.data().getPos().subtract(pos);
        if (tmp.getSquaredDistance(Vec3i.ZERO) == 1 && tmp.getComponentAlongAxis(self.axis()) != 0) {
            if (other.data().getNode() instanceof DirectionalNode axisNode) {
                return self.axis() == axisNode.axis() && axisNode.direction().getDirection().offset() == -tmp.getComponentAlongAxis(self.axis());
            } else if (other.data().getNode() instanceof AxisNode axisNode) {
                return self.axis() == axisNode.axis();
            }

            return true;
        }

        return false;
    }*/
}
