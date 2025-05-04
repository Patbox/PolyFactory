package eu.pb4.polyfactory.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polyfactory.block.fluids.transport.PipeInWallBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBlock;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WallBlock.class)
public class WallBlockMixin {
    @ModifyReturnValue(method = "shouldConnectTo", at = @At("RETURN"))
    private boolean connectToWallsWithPipes(boolean original, @Local(argsOnly = true) BlockState state, @Local(argsOnly = true) Direction side) {
        return original || (state.getBlock() instanceof PipeInWallBlock && state.get(PipeInWallBlock.AXIS) == side.rotateYClockwise().getAxis());
    }
}
