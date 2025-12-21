package eu.pb4.polyfactory.nodes.pipe;

import net.minecraft.core.Direction;

public interface FlowNode {
    boolean canFlowIn(Direction direction);
}
