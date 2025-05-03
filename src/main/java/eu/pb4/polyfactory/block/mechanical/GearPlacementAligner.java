package eu.pb4.polyfactory.block.mechanical;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;

public interface GearPlacementAligner {
    boolean isLargeGear(BlockState state);
    Direction.Axis getGearAxis(BlockState state);
}