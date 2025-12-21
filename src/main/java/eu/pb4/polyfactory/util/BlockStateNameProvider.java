package eu.pb4.polyfactory.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface BlockStateNameProvider {
    Component getName(ServerLevel world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity);
}
