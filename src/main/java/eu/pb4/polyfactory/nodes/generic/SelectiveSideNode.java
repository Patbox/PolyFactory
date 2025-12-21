package eu.pb4.polyfactory.nodes.generic;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.DirectionCheckingNode;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.Direction;

public record SelectiveSideNode(EnumSet<Direction> directions) implements DirectionCheckingNode {
    public static BlockNodeType TYPE = BlockNodeType.of(ModInit.id("selective_dir"), Codec.list(Direction.CODEC)
            .xmap(x -> new SelectiveSideNode(x.isEmpty() ? EnumSet.noneOf(Direction.class) : EnumSet.copyOf(x)), x -> List.copyOf(x.directions)));

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var list = new ArrayList<HalfLink>();
        for (var dir : directions) {
            FactoryNodes.findNodes(self, self.getBlockPos().relative(dir)).forEach(list::add);
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
