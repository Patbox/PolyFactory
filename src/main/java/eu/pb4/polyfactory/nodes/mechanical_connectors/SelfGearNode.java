package eu.pb4.polyfactory.nodes.mechanical_connectors;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.HalfLink;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.mechanical.ConveyorNode;
import eu.pb4.polyfactory.nodes.mechanical.GearMechanicalNode;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public record SelfGearNode(Direction direction, boolean flip) implements GearNode {
    public static BlockNodeType TYPE = BlockNodeType.of(ModInit.id("self_gear"), RecordCodecBuilder.<SelfGearNode>create(instance -> instance.group(
            Direction.CODEC.fieldOf("direction").forGetter(SelfGearNode::direction),
            Codec.BOOL.fieldOf("flip").forGetter(SelfGearNode::flip)
    ).apply(instance, SelfGearNode::new)));

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var list = new ArrayList<HalfLink>();
        FactoryNodes.findNodes(self, self.getBlockPos(), (self2, other) -> other.getNode() instanceof SelfGearNode).forEach(list::add);
        return list;
    }

    @Override
    public boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        return other.other().getNode() != this && other.other().getNode() instanceof SelfGearNode && self.getBlockPos().equals(other.other().getBlockPos());
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {
    }

    @Override
    public boolean isConnectedTo(NodeHolder<GearMechanicalNode> connection) {
        return connection.getNode() instanceof DirectionNode node && node.direction() == direction;
    }
}