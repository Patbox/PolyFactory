package eu.pb4.polyfactory.block.mechanical;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public interface GearPlacementAligner {
    boolean isLargeGear(BlockState state);
    Direction.Axis getGearAxis(BlockState state);
}