package eu.pb4.polyfactory.mixin.wrench;

import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(HopperBlock.class)
public class HopperBlockMixin implements WrenchableBlock {
    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public List<WrenchAction> getWrenchActions(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(WrenchAction.FACING_HOPPER);
    }
}
