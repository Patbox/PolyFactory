package eu.pb4.factorytools.api.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface RedstoneConnectable {
    boolean canRedstoneConnect(BlockState state, @Nullable Direction dir);
}
