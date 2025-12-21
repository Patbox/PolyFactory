package eu.pb4.polyfactory.mixin.wrench;

import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin({ /*PistonBlock.class,*/ DispenserBlock.class })
public class FacingBlockMixin implements ConfigurableBlock {
    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING);
    }
}
