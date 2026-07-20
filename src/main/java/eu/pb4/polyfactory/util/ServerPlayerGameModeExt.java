package eu.pb4.polyfactory.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public interface ServerPlayerGameModeExt {
    Direction polyfactory$miningDirection();
    @Nullable
    BlockPos polyfactory$currentlyDestroyed();
}
