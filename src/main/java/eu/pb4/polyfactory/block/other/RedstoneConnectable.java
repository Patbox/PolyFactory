package eu.pb4.polyfactory.block.other;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public interface RedstoneConnectable {
    boolean canRedstoneConnect(BlockState state, @Nullable Direction dir);
}
