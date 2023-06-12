package eu.pb4.polyfactory.nodes.mechanical;
/*
import com.kneelawk.graphlib.graph.BlockNode;
import com.kneelawk.graphlib.graph.BlockNodeDecoder;
import com.kneelawk.graphlib.graph.BlockNodeHolder;
import com.kneelawk.graphlib.graph.NodeView;
import com.kneelawk.graphlib.graph.struct.Node;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.AxisNode;
import eu.pb4.polyfactory.nodes.DirectionalNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public record AxisMechanicalNode(Direction.Axis axis) implements MechanicalNode, AxisNode {
    public static Identifier ID = ModInit.id("axis_mechanical");

    public static final BlockNodeDecoder DECODER = new BlockNodeDecoder() {
        @Override
        public @Nullable BlockNode createBlockNodeFromTag(@Nullable NbtElement tag) {
            return new AxisMechanicalNode(tag instanceof NbtString string ? Direction.Axis.fromName(string.asString()) : Direction.Axis.Y);
        }
    };

    @Override
    public @NotNull Identifier getTypeId() {
        return ID;
    }

    @Override
    public @Nullable NbtElement toTag() {
        return NbtString.of(axis.asString());
    }

    @Override
    public @NotNull Collection<Node<BlockNodeHolder>> findConnections(@NotNull ServerWorld world, @NotNull NodeView nodeView, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self) {
        var list = new ArrayList<Node<BlockNodeHolder>>();
        nodeView.getNodesAt(pos.offset(this.axis, 1)).filter(x -> FactoryNodes.canBothConnect(world, nodeView, self, x)).forEach(list::add);
        nodeView.getNodesAt(pos.offset(this.axis, -1)).filter(x -> FactoryNodes.canBothConnect(world, nodeView, self, x)).forEach(list::add);
        return list;
    }

    @Override
    public boolean canConnect(@NotNull ServerWorld world, @NotNull NodeView nodeView, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self, @NotNull Node<BlockNodeHolder> other) {
        return MechanicalNode.super.canConnect(world, nodeView, pos, self, other) && AxisNode.canConnect(this, pos, other);
    }

    @Override
    public void onConnectionsChanged(@NotNull ServerWorld world, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self) {}
}
*/