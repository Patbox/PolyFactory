package eu.pb4.polyfactory.nodes.generic;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.DirectionCheckingNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.core.Direction;

public record AllSideNode() implements BlockNode, DirectionCheckingNode {
    public static BlockNodeType TYPE = BlockNodeType.of(ModInit.id("all_side"), AllSideNode::new);

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var list = new ArrayList<HalfLink>();

        for (var dir : Direction.values()) {
            self.getGraphWorld().getNodesAt(self.getBlockPos().relative(dir))
                    .filter(x -> FactoryNodes.canBothConnect(self, x)).map(x -> new HalfLink(EmptyLinkKey.INSTANCE, x)).forEach(list::add);
        }
        return list;
    }

    @Override
    public boolean canConnectDir(Direction direction) {
        return true;
    }

    @Override
    public boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        return self.getBlockPos().distManhattan(other.other().getBlockPos()) == 1;
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }
}
