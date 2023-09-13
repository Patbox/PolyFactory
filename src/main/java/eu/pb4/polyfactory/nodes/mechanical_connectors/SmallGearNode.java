package eu.pb4.polyfactory.nodes.mechanical_connectors;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.DirectionCheckingNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public record SmallGearNode(Direction.Axis axis) implements BlockNode {
    public static BlockNodeType TYPE = BlockNodeType.of(ModInit.id("small_gear"),
            tag -> new SmallGearNode(tag instanceof NbtString string ? Direction.Axis.fromName(string.asString()) : Direction.Axis.Y));

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @Nullable NbtElement toTag() {
        return NbtString.of(axis.asString());
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var list = new ArrayList<HalfLink>();
        for (var dir : Direction.values()) {
            if (dir.getAxis() != axis) {
                FactoryNodes.findNodes(self, self.getBlockPos().offset(dir), (self2, other) -> other.getNode() instanceof SmallGearNode).forEach(list::add);
                FactoryNodes.findNodes(self, self.getBlockPos().offset(dir).offset(dir.rotateClockwise(axis)), (self2, other) -> other.getNode() instanceof LargeGearNode).forEach(list::add);
            }
        }

        return list;
    }

    @Override
    public boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        if (other.other().getNode() instanceof SmallGearNode smallGearNode && smallGearNode.axis == this.axis) {
            return true;
        } else if (other.other().getNode() instanceof LargeGearNode largeGearNode && largeGearNode.axis() == this.axis) {
            return true;
        }
        return false;
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {
    }

}