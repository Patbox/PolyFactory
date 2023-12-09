package eu.pb4.polyfactory.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public interface BlockStateNameProvider {
    Text getName(ServerWorld world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity);
}
