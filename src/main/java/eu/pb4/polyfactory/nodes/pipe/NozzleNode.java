package eu.pb4.polyfactory.nodes.pipe;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import net.minecraft.core.Direction;

public record NozzleNode(Direction direction) implements DirectionNode {
    public static BlockNodeType TYPE = BlockNodeType.of(ModInit.id("nozzle"), Direction.CODEC.xmap(NozzleNode::new, NozzleNode::direction));

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        return FactoryNodes.findNodes(self, self.getBlockPos().relative(this.direction)).toList();
    }

    @Override
    public boolean canFlowIn(Direction direction) {
        return this.direction.getAxis() == direction.getAxis();
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }

}
