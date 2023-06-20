package eu.pb4.polyfactory.nodes.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.CacheCategory;

public interface RotationUserNode extends BlockNode {
    CacheCategory<RotationUserNode> CACHE = CacheCategory.of(RotationUserNode.class);

}
