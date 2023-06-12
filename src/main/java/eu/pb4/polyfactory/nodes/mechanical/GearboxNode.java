package eu.pb4.polyfactory.nodes.mechanical;
/*
import com.kneelawk.graphlib.graph.BlockNode;
import com.kneelawk.graphlib.graph.BlockNodeDecoder;
import com.kneelawk.graphlib.graph.BlockNodeHolder;
import com.kneelawk.graphlib.graph.NodeView;
import com.kneelawk.graphlib.graph.struct.Node;
import com.kneelawk.graphlib.wire.FullWireBlockNode;
import com.kneelawk.graphlib.wire.WireConnectionDiscoverers;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public record GearboxNode() implements MechanicalNode, FullWireBlockNode {
    public static Identifier ID = ModInit.id("gearbox");

    public static final BlockNodeDecoder DECODER = new BlockNodeDecoder() {
        @Override
        public @Nullable BlockNode createBlockNodeFromTag(@Nullable NbtElement tag) {
            return new GearboxNode();
        }
    };

    @Override
    public @NotNull Identifier getTypeId() {
        return ID;
    }

    @Override
    public @Nullable NbtElement toTag() {
        return null;
    }

    @Override
    public @NotNull Collection<Node<BlockNodeHolder>> findConnections(@NotNull ServerWorld world, @NotNull NodeView nodeView, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self) {
        var list = new ArrayList<Node<BlockNodeHolder>>();

        for (var dir : Direction.values()) {
            var i = nodeView.getNodesAt(pos.offset(dir)).iterator();
            while (i.hasNext()) {
                var other = i.next();
                if (FactoryNodes.canBothConnect(world, nodeView, self, other)) {
                    list.add(other);
                }
            }
        }
        return list;
    }

    @Override
    public boolean canConnect(@NotNull ServerWorld world, @NotNull NodeView nodeView, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self, @NotNull Node<BlockNodeHolder> other) {
        return MechanicalNode.super.canConnect(world, nodeView, pos, self, other);
    }

    @Override
    public void onConnectionsChanged(@NotNull ServerWorld world, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self) {}
}
*/