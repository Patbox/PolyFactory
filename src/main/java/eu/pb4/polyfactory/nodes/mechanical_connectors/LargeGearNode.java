package eu.pb4.polyfactory.nodes.mechanical_connectors;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.core.Direction;

public record LargeGearNode(Direction.Axis axis) implements GearNode {
    public static BlockNodeType TYPE = BlockNodeType.of(ModInit.id("large_gear"), Direction.Axis.CODEC.xmap(LargeGearNode::new, LargeGearNode::axis));

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var list = new ArrayList<HalfLink>();
        for (var dir : Direction.values()) {
            if (dir.getAxis() != axis) {
                FactoryNodes.findNodes(self, self.getBlockPos().relative(dir).relative(dir.getClockWise(axis)), (self2, other) -> other.getNode() instanceof SmallGearNode).forEach(list::add);

                FactoryNodes.findNodes(self, self.getBlockPos().relative(dir).relative(this.axis, 1), (self2, other) -> other.getNode() instanceof LargeGearNode largeGearNode && largeGearNode.axis == dir.getAxis()).forEach(list::add);
                FactoryNodes.findNodes(self, self.getBlockPos().relative(dir).relative(this.axis, -1), (self2, other) -> other.getNode() instanceof LargeGearNode largeGearNode && largeGearNode.axis == dir.getAxis()).forEach(list::add);
            }
        }

        return list;
    }

    @Override
    public boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        if (other.other().getNode() instanceof SmallGearNode smallGearNode && smallGearNode.axis() == this.axis) {
            return true;
        } else if (other.other().getNode() instanceof LargeGearNode largeGearNode && largeGearNode.axis() != this.axis) {
            return true;
        }
        return false;
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {
    }

}