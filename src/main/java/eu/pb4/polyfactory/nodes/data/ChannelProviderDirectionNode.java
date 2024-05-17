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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


public record ChannelProviderDirectionNode(Direction direction, int channel) implements FunctionalNode, DirectionNode, DataProviderNode {
    public static final BlockNodeType TYPE = BlockNodeType.of(ModInit.id("channel/direction/provider"), RecordCodecBuilder.<ChannelProviderDirectionNode>create(instance -> instance.group(
            Direction.CODEC.fieldOf("dir").forGetter(ChannelProviderDirectionNode::direction),
            Codec.INT.fieldOf("channel").forGetter(ChannelProviderDirectionNode::channel)
    ).apply(instance, ChannelProviderDirectionNode::new)));

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        return self.getGraphWorld().getNodesAt(self.getBlockPos().offset(this.direction))
                .filter(x -> FactoryNodes.canBothConnect(self, x)).map(x -> new HalfLink(EmptyLinkKey.INSTANCE, x)).toList();
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }
}
