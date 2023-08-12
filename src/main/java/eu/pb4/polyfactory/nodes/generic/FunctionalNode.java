package eu.pb4.polyfactory.nodes.generic;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.CacheCategory;

public interface FunctionalNode extends BlockNode {
    CacheCategory<FunctionalNode> CACHE = CacheCategory.of(FunctionalNode.class);

}
