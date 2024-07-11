package eu.pb4.polyfactory.nodes.generic;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.DirectionCheckingNode;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public record UnconnectedNode() implements DirectionCheckingNode {
    public static BlockNodeType TYPE = BlockNodeType.of(ModInit.id("unconnected"), UnconnectedNode::new);

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        return List.of();
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }

    @Override
    public boolean canConnectDir(Direction direction) {
        return false;
    }
}