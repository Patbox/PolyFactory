package eu.pb4.polyfactory.nodes.generic;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.HalfLink;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.AxisNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record AxisWithDirectNode(Direction.Axis axis, List<BlockPos> connection) implements AxisNode {
    public static BlockNodeType TYPE = BlockNodeType.of(ModInit.id("axis_with_direct"), RecordCodecBuilder.<AxisWithDirectNode>create(instance -> instance.group(
            Direction.Axis.CODEC.fieldOf("axis").forGetter(AxisWithDirectNode::axis),
            BlockPos.CODEC.listOf().fieldOf("connection").forGetter(AxisWithDirectNode::connection)
    ).apply(instance, AxisWithDirectNode::new)));

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var list = new ArrayList<HalfLink>();
        FactoryNodes.findNodes(self, self.getBlockPos().offset(this.axis,1)).forEach(list::add);
        FactoryNodes.findNodes(self, self.getBlockPos().offset(this.axis,-1)).forEach(list::add);
        for (var con : connection) {
            FactoryNodes.findNodes(self, con, (x, y) -> y.getNode() instanceof AxisWithDirectNode).forEach(list::add);
        }
        return list;
    }

    @Override
    public boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        return AxisNode.super.canConnect(self, other) || this.connection.contains(other.other().getBlockPos());
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }
}