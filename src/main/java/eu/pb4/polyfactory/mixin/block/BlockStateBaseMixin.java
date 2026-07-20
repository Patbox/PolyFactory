package eu.pb4.polyfactory.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polyfactory.block.other.TagRedirector;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.MiningMode;
import eu.pb4.polyfactory.mixin.BlockBehaviourAccessor;
import eu.pb4.polyfactory.util.ServerPlayerGameModeExt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.TypedInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin implements TypedInstance<Block> {
    @Shadow public abstract Block getBlock();

    @Shadow
    protected abstract BlockState asState();

    @Intrinsic
    public boolean is(TagKey<Block> tag) {
        return TypedInstance.super.is(tag);
    }

    @Intrinsic
    public boolean is(HolderSet<Block> set) {
        return TypedInstance.super.is(set);
    }

    @ModifyReturnValue(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("TAIL"))
    private boolean extendedIsIn(boolean original, @Local(argsOnly = true) TagKey<Block> tagKey) {
        return this.getBlock() instanceof TagRedirector tagRedirector ? tagRedirector.customIsIn(tagKey, original) : original;
    }

    @ModifyReturnValue(method = "is(Lnet/minecraft/core/HolderSet;)Z", at = @At("TAIL"))
    private boolean extendedIsIn2(boolean original, @Local(argsOnly = true) HolderSet<Block> entryList) {
        return this.getBlock() instanceof TagRedirector tagRedirector ? tagRedirector.customIsIn(entryList, original) : original;
    }

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void replaceDestroyProgress(Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        var tool = player.getMainHandItem();
        if (!tool.has(FactoryDataComponents.MINING_MODE)) {
            return;
        }

        Direction direction;
        if (player instanceof ServerPlayer serverPlayer) {
            direction = ((ServerPlayerGameModeExt) serverPlayer.gameMode).polyfactory$miningDirection();
        } else {
            direction = player.getDirection();
        }

        var time = 0d;

        for (var p : tool.getOrDefault(FactoryDataComponents.MINING_MODE, MiningMode.SINGLE).positions(player, level, pos, this.asState(), direction)) {
            var s = level.getBlockState(p);
            if (MiningMode.skipMiningFor(player, level, pos, s)) continue;
            var progress = ((BlockBehaviourAccessor) s.getBlock()).callGetDestroyProgress(s, player, level, p);
            if (progress <= 0) {
                continue;
            }

            time += 1 / progress;
        }

        cir.setReturnValue(time != 0 ? (float) (1 / time) : Float.MAX_VALUE);
    }
}
