package eu.pb4.polyfactory.nodes.generic;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.HalfLink;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.AxisNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;

public record DirectNode(List<BlockPos> connection) implements BlockNode {
    public static BlockNodeType TYPE = BlockNodeType.of(ModInit.id("direct"), RecordCodecBuilder.<DirectNode>create(instance -> instance.group(
            BlockPos.CODEC.listOf().fieldOf("connection").forGetter(DirectNode::connection)
    ).apply(instance, DirectNode::new)));

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var list = new ArrayList<HalfLink>();
        for (var con : connection) {
            FactoryNodes.findNodes(self, con, (x, y) -> y.getNode() instanceof DirectNode).forEach(list::add);
        }
        return list;
    }

    @Override
    public boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        return this.connection.contains(other.other().getBlockPos());
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }
}