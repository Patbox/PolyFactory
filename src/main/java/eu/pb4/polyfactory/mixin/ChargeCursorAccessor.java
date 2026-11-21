package eu.pb4.polyfactory.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSpreader;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.level.block.SculkSpreader.ChargeCursor.class)
public interface ChargeCursorAccessor {
    @Invoker
    static BlockPos callGetValidMovementPos(final LevelAccessor level, final BlockPos pos, final RandomSource random, final BlockPos originPos, final SculkSpreader spreader) {
        throw new UnsupportedOperationException();
    }
}
