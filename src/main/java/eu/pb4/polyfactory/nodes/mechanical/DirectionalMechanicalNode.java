package eu.pb4.polyfactory.nodes.mechanical;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeDecoder;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.ModInit;
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

public record DirectionalMechanicalNode(Direction direction) implements MechanicalNode, DirectionalNode {
    public static Identifier ID = ModInit.id("directional_mechanical_input");

    public static final BlockNodeDecoder DECODER = new BlockNodeDecoder() {
        @Override
        public @Nullable BlockNode decode(@Nullable NbtElement tag) {
            return new DirectionalMechanicalNode(tag instanceof NbtString string ? Direction.byName(string.asString()) : Direction.NORTH);
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
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        return self.getGraphWorld().getNodesAt(self.getPos().offset(this.direction)).filter(x -> FactoryNodes.canBothConnect(self, x)).map(x -> new HalfLink(EmptyLinkKey.INSTANCE, x)).toList();
    }

    @Override
    public boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        return MechanicalNode.super.canConnect(self, other) && DirectionalNode.canConnect(this, self, other);
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }

}
