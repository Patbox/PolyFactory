package eu.pb4.polyfactory.nodes.generic;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.AxisNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.core.Direction;

public record FunctionalAxisNode(Direction.Axis axis) implements FunctionalNode, AxisNode {
    public static BlockNodeType TYPE = BlockNodeType.of(ModInit.id("axis/functional"), Direction.Axis.CODEC.xmap(FunctionalAxisNode::new, FunctionalAxisNode::axis));

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var list = new ArrayList<HalfLink>();
        self.getGraphWorld().getNodesAt(self.getBlockPos().relative(this.axis,1))
                .filter(x -> FactoryNodes.canBothConnect(self, x)).map(x -> new HalfLink(EmptyLinkKey.INSTANCE, x)).forEach(list::add);
        self.getGraphWorld().getNodesAt(self.getBlockPos().relative(this.axis,-1))
                .filter(x -> FactoryNodes.canBothConnect(self, x)).map(x -> new HalfLink(EmptyLinkKey.INSTANCE, x)).forEach(list::add);

        return list;
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }

}