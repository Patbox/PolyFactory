package eu.pb4.polyfactory.nodes.pipe;

import net.minecraft.util.math.Direction;

public interface FlowNode {
    boolean canFlowIn(Direction direction);
}
