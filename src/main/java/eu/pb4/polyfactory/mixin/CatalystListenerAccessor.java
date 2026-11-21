package eu.pb4.polyfactory.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.level.block.entity.SculkCatalystBlockEntity.CatalystListener.class)
public interface CatalystListenerAccessor {
    @Invoker
    void callBloom(final ServerLevel level, final BlockPos pos, final BlockState state, final RandomSource random);

    @Accessor
    SculkSpreader getSculkSpreader();
}
