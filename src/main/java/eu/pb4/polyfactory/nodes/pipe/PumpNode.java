package eu.pb4.polyfactory.nodes.pipe;


import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.CacheCategory;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.AxisNode;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.generic.FunctionalNode;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;


public record PumpNode(Direction flowDirection, boolean isPulling, Direction direction, int range) implements FunctionalNode, DirectionNode {
    public static final int DEFAULT_RANGE = 32;
    public static final int SPOUT_RANGE = 8;

    public static final Codec<PumpNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Direction.CODEC.fieldOf("direction").forGetter(PumpNode::flowDirection),
            Codec.BOOL.fieldOf("reverse").forGetter(PumpNode::isPulling),
            Codec.INT.optionalFieldOf("range", DEFAULT_RANGE).forGetter(PumpNode::range)
    ).apply(instance, PumpNode::new));

    public static final CacheCategory<PumpNode> CACHE = CacheCategory.of(PumpNode.class);
    public PumpNode(Direction flowDirection, boolean reverse, int maxStrength) {
        this(flowDirection, reverse, reverse ? flowDirection.getOpposite() : flowDirection, maxStrength);
    }

    public static final BlockNodeType TYPE = BlockNodeType.of(ModInit.id("pump"), CODEC);

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
