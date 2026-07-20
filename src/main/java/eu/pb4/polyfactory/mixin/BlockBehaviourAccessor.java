package eu.pb4.polyfactory.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.level.block.state.BlockBehaviour.class)
public interface BlockBehaviourAccessor {
    @Invoker
    float callGetDestroyProgress(final BlockState state, final Player player, final BlockGetter level, final BlockPos pos);
}
