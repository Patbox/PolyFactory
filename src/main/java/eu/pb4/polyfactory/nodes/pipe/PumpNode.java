package eu.pb4.polyfactory.nodes.pipe;


import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.CacheCategory;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.generic.FunctionalNode;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;


public record PumpNode(Direction flowDirection, boolean isPulling, Direction direction) implements FunctionalNode, DirectionNode {
    public static final CacheCategory<PumpNode> CACHE = CacheCategory.of(PumpNode.class);
    public PumpNode(Direction flowDirection, boolean reverse) {
        this(flowDirection, reverse, reverse ? flowDirection.getOpposite() : flowDirection);
    }

    public static final BlockNodeType TYPE = BlockNodeType.of(ModInit.id("pump"), Codec.mapPair(Direction.CODEC.fieldOf("direction"), Codec.BOOL.fieldOf("reverse"))
            .xmap(x -> new PumpNode(x.getFirst(), x.getSecond()), x -> new Pair<>(x.flowDirection, x.isPulling)).codec());

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
