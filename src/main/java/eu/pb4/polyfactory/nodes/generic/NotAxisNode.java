package eu.pb4.polyfactory.nodes.generic;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.AxisNode;
import eu.pb4.polyfactory.nodes.DirectionCheckingNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.core.Direction;

public record NotAxisNode(Direction.Axis axis) implements DirectionCheckingNode {
    public static BlockNodeType TYPE = BlockNodeType.of(ModInit.id("not_axis"), Direction.Axis.CODEC.xmap(NotAxisNode::new, NotAxisNode::axis));

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var list = new ArrayList<HalfLink>();
        for (var dir : Direction.values()) {
            if (dir.getAxis() != axis) {
                FactoryNodes.findNodes(self, self.getBlockPos().relative(dir)).forEach(list::add);
            }
        }
        return list;
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }

    @Override
    public boolean canConnectDir(Direction direction) {
        return direction.getAxis() != axis;
    }
}