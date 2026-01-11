package eu.pb4.polyfactory.nodes.mechanical_connectors;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.nodes.mechanical.GearMechanicalNode;

public interface GearNode extends BlockNode {
    default boolean isConnectedTo(NodeHolder<GearMechanicalNode> connection) {
        return true;
    }
}