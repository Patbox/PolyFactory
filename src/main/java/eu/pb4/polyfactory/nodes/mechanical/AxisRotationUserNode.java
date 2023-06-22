package eu.pb4.polyfactory.nodes.mechanical;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeDecoder;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.AxisNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public record AxisRotationUserNode(Direction.Axis axis) implements MechanicalNode, RotationUserNode, AxisNode {
    public static BlockNodeType TYPE = BlockNodeType.of(ModInit.id("axis_rotation_user"),
            tag -> new AxisRotationUserNode(tag instanceof NbtString string ? Direction.Axis.fromName(string.asString()) : Direction.Axis.Y));

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
        self.getGraphWorld().getNodesAt(self.getBlockPos().offset(this.axis,1))
                .filter(x -> FactoryNodes.canBothConnect(self, x)).map(x -> new HalfLink(EmptyLinkKey.INSTANCE, x)).forEach(list::add);
        self.getGraphWorld().getNodesAt(self.getBlockPos().offset(this.axis,-1))
                .filter(x -> FactoryNodes.canBothConnect(self, x)).map(x -> new HalfLink(EmptyLinkKey.INSTANCE, x)).forEach(list::add);

        return list;
    }

    @Override
    public boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        return MechanicalNode.super.canConnect(self, other) && AxisNode.canConnect(this, self, other);
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }

}