package eu.pb4.polyfactory.nodes.generic;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.nodes.DirectionCheckingNode;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

public record SelectiveSideNode(EnumSet<Direction> directions) implements DirectionCheckingNode {
    public static BlockNodeType TYPE = BlockNodeType.of(ModInit.id("selective_dir"), SelectiveSideNode::decode);

    private static BlockNode decode(NbtElement element) {
        var out = new ArrayList<Direction>();
        var list = element instanceof NbtList list1 ? list1 : new NbtList();

        for (var entry : list) {
            if (entry instanceof NbtString string) {
                out.add(Direction.byName(string.asString()));
            }
        }
        return new SelectiveSideNode(EnumSet.copyOf(out));
    }

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @Nullable NbtElement toTag() {
        var list = new NbtList();
        for (var dir : directions) {
            list.add(NbtString.of(dir.asString()));
        }
        return list;
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
