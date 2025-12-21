package eu.pb4.polyfactory.nodes.data;


import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.CacheCategory;
import com.kneelawk.graphlib.api.util.HalfLink;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.DirectionCheckingNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.generic.FunctionalNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.Direction;


public record SpeakerNode(Direction facing, EnumSet<Direction> cableDir, int channel) implements FunctionalNode, DirectionCheckingNode, DataReceiverNode {
    public static final CacheCategory<SpeakerNode> CACHE = CacheCategory.of(SpeakerNode.class);

    public static final BlockNodeType TYPE = BlockNodeType.of(ModInit.id("speaker"), RecordCodecBuilder.<SpeakerNode>create(instance -> instance.group(
            Direction.CODEC.fieldOf("facing").forGetter(SpeakerNode::facing),
            Direction.CODEC.listOf().xmap(x -> x.isEmpty() ? EnumSet.noneOf(Direction.class) : EnumSet.copyOf(x), List::copyOf)
                    .fieldOf("cable").forGetter(SpeakerNode::cableDir),
            Codec.INT.fieldOf("channel").forGetter(SpeakerNode::channel)
    ).apply(instance, SpeakerNode::new)));

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var list = new ArrayList<HalfLink>();
        for (var dir : cableDir) {
            FactoryNodes.findNodes(self, self.getBlockPos().relative(dir)).forEach(list::add);
        }
        return list;
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {
    }

    @Override
    public boolean canConnectDir(Direction direction) {
        return this.cableDir.contains(direction);
    }
}
