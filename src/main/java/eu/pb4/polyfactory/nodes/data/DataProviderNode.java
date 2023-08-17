package eu.pb4.polyfactory.nodes.data;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.util.CacheCategory;

public interface DataProviderNode extends BlockNode {
    int channel();
}
