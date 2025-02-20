package eu.pb4.polyfactory.nodes.data;


import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.HalfLink;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.nodes.DirectionCheckingNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.generic.FunctionalNode;
import eu.pb4.polyfactory.nodes.generic.SelectiveSideNode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;


public record ChannelProviderSelectiveSideNode(EnumSet<Direction> directions, int channel) implements FunctionalNode, DirectionCheckingNode, DataProviderNode {
    public static final BlockNodeType TYPE = BlockNodeType.of(ModInit.id("channel/selective_dir/provider"), RecordCodecBuilder.<ChannelProviderSelectiveSideNode>create(instance -> instance.group(
            Direction.CODEC.listOf().xmap(x -> x.isEmpty() ? EnumSet.noneOf(Direction.class) : EnumSet.copyOf(x), List::copyOf)
                    .fieldOf("dir").forGetter(ChannelProviderSelectiveSideNode::directions),
            Codec.INT.fieldOf("channel").forGetter(ChannelProviderSelectiveSideNode::channel)
    ).apply(instance, ChannelProviderSelectiveSideNode::new)));

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var list = new ArrayList<HalfLink>();
        for (var dir : directions) {
            FactoryNodes.findNodes(self, self.getBlockPos().offset(dir)).forEach(list::add);
        }

        return list;
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {
    }

    @Override
    public boolean canConnectDir(Direction direction) {
        return this.directions.contains(direction);
    }
}
