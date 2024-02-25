package eu.pb4.polyfactory.nodes.data;


import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.DirectionCheckingNode;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.generic.FunctionalNode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;


public record ChannelReceiverSelectiveSideNode(EnumSet<Direction> directions, int channel) implements FunctionalNode, DirectionCheckingNode, DataReceiverNode {
    public static final BlockNodeType TYPE = BlockNodeType.of(ModInit.id("channel/selective_dir/receiver"),
            ChannelReceiverSelectiveSideNode::fromNbt);

    private static BlockNode fromNbt(NbtElement element) {
        if (element instanceof NbtCompound compound) {
            var out = new ArrayList<Direction>();
            var list = compound.getList("dir", NbtElement.STRING_TYPE);

            for (var entry : list) {
                if (entry instanceof NbtString string) {
                    out.add(Direction.byName(string.asString()));
                }
            }

            return new ChannelReceiverSelectiveSideNode(out.isEmpty() ? EnumSet.noneOf(Direction.class) : EnumSet.copyOf(out), compound.getInt("channel"));
        }
        return new ChannelReceiverSelectiveSideNode(EnumSet.noneOf(Direction.class),  0);
    }

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @Nullable NbtElement toTag() {
        var nbt = new NbtCompound();
        var list = new NbtList();
        for (var dir : directions) {
            list.add(NbtString.of(dir.asString()));
        }

        nbt.put("dir", list);
        nbt.putInt("channel", channel);
        return nbt;
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        var list = new ArrayList<HalfLink>();
        for (var dir : directions) {
            FactoryNodes.findNodes(self, self.getBlockPos().offset(dir)).forEach(list::add);
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
