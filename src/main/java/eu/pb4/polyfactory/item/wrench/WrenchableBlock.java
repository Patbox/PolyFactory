package eu.pb4.polyfactory.item.wrench;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

public interface WrenchableBlock {
    List<WrenchAction> getWrenchActions(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state);

    default void wrenchTick(ServerPlayerEntity player, BlockHitResult hit, BlockState state) {}
}
