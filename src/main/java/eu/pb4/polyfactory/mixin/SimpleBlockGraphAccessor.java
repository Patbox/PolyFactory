package eu.pb4.polyfactory.mixin;

import com.google.common.collect.Multimap;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.impl.graph.simple.SimpleBlockGraph;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleBlockGraph.class)
public interface SimpleBlockGraphAccessor {
    @Accessor
    Multimap<BlockPos, NodeHolder<BlockNode>> getNodesInPos();
}
