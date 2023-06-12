package eu.pb4.polyfactory.nodes.mechanical;
/*
import com.kneelawk.graphlib.graph.BlockNode;
import com.kneelawk.graphlib.graph.BlockNodeDecoder;
import com.kneelawk.graphlib.graph.BlockNodeHolder;
import com.kneelawk.graphlib.graph.NodeView;
import com.kneelawk.graphlib.graph.struct.Node;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.electric.ElectricMotorBlock;
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

import java.util.Collection;
import java.util.Collections;

public record RotationalSourceNode(Direction direction) implements MechanicalNode, DirectionalNode {
    public static final Identifier ID = ModInit.id("rotational_source");
    public static final BlockNodeDecoder DECODER = new BlockNodeDecoder() {
        @Override
        public @Nullable BlockNode createBlockNodeFromTag(@Nullable NbtElement tag) {
            return new RotationalSourceNode(tag instanceof NbtString string ? Direction.byName(string.asString()) : Direction.NORTH);
        }
    };

    @Override
    public @NotNull Identifier getTypeId() {
        return ID;
    }

    @Override
    public @Nullable NbtElement toTag() {
        return NbtString.of(direction.asString());
    }

    @Override
    public @NotNull Collection<Node<BlockNodeHolder>> findConnections(@NotNull ServerWorld world, @NotNull NodeView nodeView, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self) {
        return nodeView.getNodesAt(pos.offset(direction)).filter(x -> FactoryNodes.canBothConnect(world, nodeView, self, x)).toList();
    }

    @Override
    public boolean canConnect(@NotNull ServerWorld world, @NotNull NodeView nodeView, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self, @NotNull Node<BlockNodeHolder> other) {
        return other.data().getNode() instanceof MechanicalNode && !(other.data().getNode() instanceof RotationalSourceNode) && !(other.data().getNode() instanceof ConveyorNode && direction == Direction.DOWN);
    }

    @Override
    public void onConnectionsChanged(@NotNull ServerWorld world, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self) {

    }
}
*/