package eu.pb4.polyfactory.mixin.fluid;

import eu.pb4.polyfactory.block.fluids.PipeConnectable;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractCauldronBlock.class)
public class AbstractCauldronBlockMixin implements PipeConnectable {

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public boolean canPipeConnect(WorldAccess world, BlockPos pos, BlockState state, Direction dir) {
        return true;
    }
}
