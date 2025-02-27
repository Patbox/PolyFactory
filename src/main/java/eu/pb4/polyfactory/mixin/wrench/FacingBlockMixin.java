package eu.pb4.polyfactory.mixin.wrench;

import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin({ /*PistonBlock.class,*/ DispenserBlock.class })
public class FacingBlockMixin implements WrenchableBlock {
    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public List<WrenchAction> getWrenchActions(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(WrenchAction.FACING);
    }
}
