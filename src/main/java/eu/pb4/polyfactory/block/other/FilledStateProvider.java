package eu.pb4.polyfactory.block.other;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface FilledStateProvider {
    @Nullable
    Component getFilledStateText();

    long getFilledAmount();
    long getFillCapacity();

    interface Remote {
        @Nullable
        FilledStateProvider getFilledStateProvider(ServerLevel level, BlockPos pos, BlockState blockState, @Nullable BlockEntity blockEntity);
    }
}
