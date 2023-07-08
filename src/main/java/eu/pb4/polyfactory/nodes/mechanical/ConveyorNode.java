package eu.pb4.polyfactory.nodes.mechanical;

import com.kneelawk.graphlib.api.graph.GraphView;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeDecoder;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import eu.pb4.polyfactory.nodes.AxisNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public record ConveyorNode(Direction direction, ConveyorBlock.DirectionValue value) implements MechanicalNode, AxisNode {
    public static final BlockNodeType TYPE = BlockNodeType.of(ModInit.id("conveyor"), tag -> {
        if (tag == null) {
            return null;
        }

        var compound = (NbtCompound) tag;
        return new ConveyorNode(Direction.byName(compound.getString("direction")), ConveyorBlock.DirectionValue.valueOf(compound.getString("value").toUpperCase(Locale.ROOT)));
    });

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @Nullable NbtElement toTag() {
        var nbt = new NbtCompound();
        nbt.putString("direction", direction.getName());
        nbt.putString("value", value.asString());
        return nbt;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var nextPos = new BlockPos.Mutable();
        var list = new ArrayList<HalfLink>();

        var view = self.getGraphWorld();
        var pos = self.getBlockPos();

        Predicate<NodeHolder<BlockNode>> conv = other -> (other.getNode() instanceof ConveyorNode node && node.direction == this.direction);
        Predicate<NodeHolder<BlockNode>> sides = other -> FactoryNodes.canBothConnect(self, other);

        if (this.value.value == 0) {
            nextPos.set(pos).move(direction, -1);
            addNodes(view, nextPos, conv, list);
            nextPos.move(direction, 2);
            addNodes(view, nextPos, conv, list);

            nextPos.set(pos).move(0, 1, 0);
            addNodes(view, nextPos, conv, list);
            nextPos.move(0, -2, 0);
            addNodes(view, nextPos, conv, list);
        } else if (this.value.stack) {
            nextPos.set(pos).move(0, 1, 0);
            addNodes(view, nextPos, conv, list);
            nextPos.move(0, -2, 0);
            addNodes(view, nextPos, conv, list);
        } else {
            nextPos.set(pos).move(direction, this.value.value);
            addNodes(view, nextPos, conv, list);
            nextPos.set(pos).move(0, -1, 0);
            addNodes(view, nextPos, conv, list);
        }

        var side = direction.rotateYClockwise();

        nextPos.set(pos).move(side, 1);
        addNodes(view, nextPos, sides, list);
        nextPos.move(side, -2);
        addNodes(view, nextPos, sides, list);


        return list;
    }

    private void addNodes(GraphView view, BlockPos pos, Predicate<NodeHolder<BlockNode>> predicate, List<HalfLink> list) {
        view.getNodesAt(pos).filter(predicate).forEach((a) -> list.add(new HalfLink(EmptyLinkKey.INSTANCE, a)));
    }

    @Override
    public boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        return MechanicalNode.super.canConnect(self, other)
                && ((other.other().getNode() instanceof ConveyorNode conv && conv.direction == this.direction) || AxisNode.canConnect(this, self, other));
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }

    @Override
    public Direction.Axis axis() {
        return this.direction.rotateYClockwise().getAxis();
    }
}
