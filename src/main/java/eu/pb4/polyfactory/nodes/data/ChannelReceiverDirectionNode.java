package eu.pb4.polyfactory.nodes.data;


import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.generic.FunctionalNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import net.minecraft.core.Direction;


public record ChannelReceiverDirectionNode(Direction direction, int channel, boolean instant) implements FunctionalNode, DirectionNode, DataReceiverNode {
    public static final BlockNodeType TYPE = BlockNodeType.of(ModInit.id("channel/direction/receiver"), RecordCodecBuilder.<ChannelReceiverDirectionNode>create(instance -> instance.group(
            Direction.CODEC.fieldOf("dir").forGetter(ChannelReceiverDirectionNode::direction),
            Codec.INT.fieldOf("channel").forGetter(ChannelReceiverDirectionNode::channel),
            Codec.BOOL.optionalFieldOf("instant", false).forGetter(ChannelReceiverDirectionNode::instant)
    ).apply(instance, ChannelReceiverDirectionNode::new)));

    public ChannelReceiverDirectionNode(Direction direction, int channel) {
        this(direction, channel, false);
    }

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        return self.getGraphWorld().getNodesAt(self.getBlockPos().relative(this.direction))
                .filter(x -> FactoryNodes.canBothConnect(self, x)).map(x -> new HalfLink(EmptyLinkKey.INSTANCE, x)).toList();
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }
}
