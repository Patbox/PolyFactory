package eu.pb4.polyfactory.compat.power_networks;

import com.kneelawk.graphlib.api.graph.NodeHolder;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.kneelawk.graphlib.api.graph.user.BlockNodeType;
import com.kneelawk.graphlib.api.util.EmptyLinkKey;
import com.kneelawk.graphlib.api.util.HalfLink;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.electric.EnergyUser;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.electric.EnergyData;
import eu.pb4.polyfactory.nodes.generic.FunctionalNode;
import io.github.mattidragon.powernetworks.PowerNetworks;
import io.github.mattidragon.powernetworks.block.CoilBlock;
import io.github.mattidragon.powernetworks.misc.CoilTransferMode;
import io.github.mattidragon.powernetworks.network.NetworkRegistry;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorageUtil;

import java.util.Collection;
import java.util.List;

public record PowerNetworksNode(Direction direction) implements FunctionalNode, DirectionNode, EnergyUser {
    public static final BlockNodeType TYPE = BlockNodeType.of(ModInit.id("compat/power_networks"),
            tag -> new PowerNetworksNode(tag instanceof NbtString string ? Direction.byName(string.asString()) : Direction.NORTH));

    @Override
    public @NotNull BlockNodeType getType() {
        return TYPE;
    }

    @Override
    public @Nullable NbtElement toTag() {
        return NbtString.of(direction.asString());
    }

    @Override
    public @NotNull Collection<HalfLink> findConnections(@NotNull NodeHolder<BlockNode> self) {
        return self.getGraphWorld().getNodesAt(self.getBlockPos().offset(this.direction))
                .filter(x -> FactoryNodes.canBothConnect(self, x)).map(x -> new HalfLink(EmptyLinkKey.INSTANCE, x)).toList();
    }

    @Override
    public boolean canConnect(@NotNull NodeHolder<BlockNode> self, @NotNull HalfLink other) {
        return DirectionNode.canConnect(this, self, other);
    }

    @Override
    public Object getTargetFunctional(ServerWorld world, BlockPos pos, BlockState state) {
        return this;
    }

    @Override
    public void onConnectionsChanged(@NotNull NodeHolder<BlockNode> self) {

    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void updateEnergyData(EnergyData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        var be = CoilBlock.getBlockEntity(world, pos);
        if (be == null) {
            return;
        }

        if (be.getTransferMode() == CoilTransferMode.INPUT) {
            try (var t = Transaction.openOuter()) {
                var amount = be.storage.insert(modifier.data.current(), t);
                t.commit();
                modifier.use(amount);
            }
        } else if (be.getTransferMode() == CoilTransferMode.OUTPUT) {
            try (var t = Transaction.openOuter()) {
                var amount = be.storage.extract(be.getTier().getTransferRate() / 10, t);
                t.commit();
                modifier.provide(amount);
            }
        }
    }

    @Override
    public Collection<BlockNode> createEnergyNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(this);
    }
}
