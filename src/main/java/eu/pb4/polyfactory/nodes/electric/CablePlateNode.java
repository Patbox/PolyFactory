package eu.pb4.polyfactory.nodes.electric;
/*
import com.kneelawk.graphlib.graph.BlockNode;
import com.kneelawk.graphlib.graph.BlockNodeDecoder;
import com.kneelawk.graphlib.graph.BlockNodeHolder;
import com.kneelawk.graphlib.graph.NodeView;
import com.kneelawk.graphlib.graph.struct.Node;
import com.kneelawk.graphlib.wire.SidedWireBlockNode;
import com.kneelawk.graphlib.wire.WireConnectionDiscoverers;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.DirectionalNode;
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

public record CablePlateNode(Direction side) implements ElectricalNode, SidedWireBlockNode {
    public static Identifier ID = ModInit.id("cable_plate");

    public static final BlockNodeDecoder DECODER = new BlockNodeDecoder() {
        @Override
        public @Nullable BlockNode createBlockNodeFromTag(@Nullable NbtElement tag) {
            return new CablePlateNode(tag instanceof NbtString string ? Direction.byName(string.asString()) : Direction.NORTH);
        }
    };

    @Override
    public @NotNull Identifier getTypeId() {
        return ID;
    }

    @Override
    public @Nullable NbtElement toTag() {
        return NbtString.of(side.asString());
    }

    @Override
    public @NotNull Collection<Node<BlockNodeHolder>> findConnections(@NotNull ServerWorld world, @NotNull NodeView nodeView, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self) {
        var list = new ArrayList<Node<BlockNodeHolder>>();
        list.addAll(WireConnectionDiscoverers.wireFindConnections(this, world, nodeView, pos,self, null));

        for (var x : nodeView.getNodesAt(pos.offset(this.side.getOpposite())).toList()) {
            if (!(x.data().getNode() instanceof ElectricalNode)) {
                continue;
            }

            if (x.data().getNode() instanceof CablePlateNode cablePlateNode) {
                if (cablePlateNode.side != this.side) {
                    list.add(x);
                }
            } else if (x.data().getNode() instanceof DirectionalNode directionalNode) {
                if (directionalNode.direction() == this.side) {
                    list.add(x);
                }
            } else {
                list.add(x);
            }
        }

        for (var dir : Direction.values()) {
            if (dir.getAxis() == this.side.getAxis()) {
                continue;
            }
            for (var x : nodeView.getNodesAt(pos.offset(dir)).toList()) {
                if (!(x.data().getNode() instanceof ElectricalNode)) {
                    continue;
                }

                if (x.data().getNode() instanceof CablePlateNode cablePlateNode) {
                    if (cablePlateNode.side.getAxis() != this.side.getAxis() && cablePlateNode.side.getOpposite() != dir) {
                        list.add(x);
                    }
                } else if (x.data().getNode() instanceof DirectionalNode directionalNode) {
                    if (directionalNode.direction() == dir.getOpposite()) {
                        list.add(x);
                    }
                } else {
                    list.add(x);
                }
            }
        }

        return list;
    }

    @Override
    public void onConnectionsChanged(@NotNull ServerWorld world, @NotNull BlockPos pos, @NotNull Node<BlockNodeHolder> self) {

    }

    public @NotNull Direction getSide() {
        return this.side.getOpposite();
    }
}
*/