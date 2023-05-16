package eu.pb4.polyfactory.nodes.mechanical;

import com.kneelawk.graphlib.graph.BlockNode;
import com.kneelawk.graphlib.graph.BlockNodeDecoder;
import com.kneelawk.graphlib.graph.BlockNodeHolder;
import com.kneelawk.graphlib.graph.NodeView;
import com.kneelawk.graphlib.graph.struct.Node;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import eu.pb4.polyfactory.nodes.DirectionalNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public record ConveyorNode(Direction direction, ConveyorBlock.DirectionValue value) implements MechanicalNode {
    public static final Identifier ID = ModInit.id("conveyor");
    public static final BlockNodeDecoder DECODER = new BlockNodeDecoder() {
        @Override
        public @Nullable BlockNode createBlockNodeFromTag(@Nullable NbtElement tag) {
            if (tag == null) {
                return null;
            }

            var compound = (NbtCompound) tag;
            return new ConveyorNode(Direction.byName(compound.getString("direction")), ConveyorBlock.DirectionValue.valueOf(compound.getString("value").toUpperCase(Locale.ROOT)));
        }
    };

    @Override
    public @NotNull Identifier getTypeId() {
        return ID;
    }

    @Override
    public @Nullable NbtElement toTag() {
        var nbt = new NbtCompound();
        nbt.putString("direction", direction.getName());
        nbt.putString("value", value.asString());
        return nbt;
    }


    // Todo, rewrite this, it could be way shorter
    @Override
    public @NotNull Collection<Node<BlockNodeHolder>> findConnections(@NotNull ServerWorld world, @NotNull NodeView view, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self) {
        var nextPos = new BlockPos.Mutable();
        var list = new ArrayList<Node<BlockNodeHolder>>();
        Predicate<Node<BlockNodeHolder>> conv = other -> (other.data().getNode() instanceof ConveyorNode node && node.direction == this.direction) && FactoryNodes.canBothConnect(world, view, self, other);
        Predicate<Node<BlockNodeHolder>> convType = other -> (other.data().getNode() instanceof ConveyorNode node && node.direction == this.direction && node.value.value == this.value.value) && FactoryNodes.canBothConnect(world, view, self, other);
        Predicate<Node<BlockNodeHolder>> convTypeUp = other -> (other.data().getNode() instanceof ConveyorNode node && node.direction == this.direction && node.value.value == 1) && FactoryNodes.canBothConnect(world, view, self, other);
        Predicate<Node<BlockNodeHolder>> convTypeDown = other -> (other.data().getNode() instanceof ConveyorNode node && node.direction == this.direction && node.value.value == -1) && FactoryNodes.canBothConnect(world, view, self, other);
        Predicate<Node<BlockNodeHolder>> p = other -> !(other.data().getNode() instanceof ConveyorNode node && node.direction != this.direction) && FactoryNodes.canBothConnect(world, view, self, other);

        if (this.value.value == 0) {
            nextPos.set(pos).move(direction, -1);
            addNodes(view, nextPos, conv, list);
            nextPos.move(Direction.UP, 1);
            addNodes(view, nextPos, convTypeDown, list);

            nextPos.move(direction, 2);
            addNodes(view, nextPos, conv, list);
            nextPos.move(Direction.UP, 1);
            addNodes(view, nextPos, convTypeUp, list);

        } else {
            nextPos.set(pos).move(direction, this.value.value);
            addNodes(view, nextPos, conv, list);
            addNodes(view, nextPos.move(0, 1, 0), conv, list);

            nextPos.set(pos).move(direction, -this.value.value).move(0, -1, 0);
            addNodes(view, nextPos, conv, list);


            if (this.value.stack) {
                nextPos.set(pos).move(0, -1, 0);
                addNodes(view, nextPos, convType, list);
                nextPos.move(0, 2, 0);
                addNodes(view, nextPos, convType, list);
            }
        }


        for (var x : Direction.values()) {
            if (x == Direction.UP) {
                continue;
            }
            nextPos.set(pos).move(x);

            view.getNodesAt(nextPos).filter(p).forEach(list::add);
        }

        return list;
    }

    private void addNodes(NodeView view, BlockPos pos, Predicate<Node<BlockNodeHolder>> predicate, List<Node<BlockNodeHolder>> list) {
        view.getNodesAt(pos).filter(predicate).forEach(list::add);
    }

    @Override
    public boolean canConnect(@NotNull ServerWorld world, @NotNull NodeView nodeView, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self, @NotNull Node<BlockNodeHolder> other) {
        return MechanicalNode.super.canConnect(world, nodeView, pos, self, other) && (other.data().getNode() instanceof ConveyorNode || other.data().getPos().getY() - pos.getY() != 1);
    }

    @Override
    public void onConnectionsChanged(@NotNull ServerWorld world, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self) {

    }
}
