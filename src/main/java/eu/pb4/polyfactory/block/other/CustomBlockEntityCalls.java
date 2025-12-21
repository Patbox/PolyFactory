package eu.pb4.polyfactory.block.other;

import net.minecraft.world.level.block.state.BlockState;

public interface CustomBlockEntityCalls {
    default void polyfactory$setCachedState(BlockState blockState) {}
    default void polyfactory$markRemoved() {}
}
