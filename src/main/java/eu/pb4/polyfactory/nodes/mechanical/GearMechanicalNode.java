package eu.pb4.polyfactory.nodes.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.CacheCategory;

public interface GearMechanicalNode extends BlockNode {
    CacheCategory<GearMechanicalNode> CACHE = CacheCategory.of(GearMechanicalNode.class);
}
