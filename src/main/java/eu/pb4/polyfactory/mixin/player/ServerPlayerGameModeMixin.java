package eu.pb4.polyfactory.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.component.MiningMode;
import eu.pb4.polyfactory.util.ServerPlayerGameModeExt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin implements ServerPlayerGameModeExt {
    @Shadow @Final protected ServerPlayer player;

    @Shadow
    protected ServerLevel level;
    @Unique
    private Direction miningDirection = Direction.UP;
    @Unique
    private BlockPos currentlyDestroyed = null;

    @ModifyExpressionValue(
            method = "useItemOn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSecondaryUseActive()Z")
    )
    private boolean dontCancelForSome(boolean original) {
        if (this.player.getMainHandItem().is(FactoryItems.WRENCH)) {
            return true;
        }
        return original;
    }

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void storeDirection(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int maxY, int sequence, CallbackInfo ci) {
        this.miningDirection = direction;
    }

    @Override
    public Direction polyfactory$miningDirection() {
        return this.miningDirection;
    }

    @Override
    public @Nullable BlockPos polyfactory$currentlyDestroyed() {
        return this.currentlyDestroyed;
    }

    @WrapOperation(method = "handleBlockBreakAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isUnderSpawnProtection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean handleMultiDestroySpawnProtection(MinecraftServer instance, ServerLevel level, BlockPos pos, Player player, Operation<Boolean> original) {
        var tool = player.getMainHandItem();
        if (!tool.has(FactoryDataComponents.SELECTED_MINING_MODE)) {
            return original.call(instance, level, pos, player);
        }

        for (var p : tool.getOrDefault(FactoryDataComponents.SELECTED_MINING_MODE, MiningMode.SINGLE).positions(player, this.level, pos, level.getBlockState(pos), this.miningDirection)) {
            if (MiningMode.skipMiningFor(player, level, pos, level.getBlockState(p))) continue;
            if (original.call(instance, level, p, player)) {
                return true;
            }
        }
        return false;
    }

    @WrapOperation(method = "handleBlockBreakAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;mayInteract(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean handleMultiDestroyMayInteract(ServerLevel instance, Entity entity, BlockPos pos, Operation<Boolean> original) {
        var tool = player.getMainHandItem();
        if (!tool.has(FactoryDataComponents.SELECTED_MINING_MODE)) {
            return original.call(instance, entity, pos);
        }

        for (var p : tool.getOrDefault(FactoryDataComponents.SELECTED_MINING_MODE, MiningMode.SINGLE).positions(player, this.level, pos, level.getBlockState(pos), this.miningDirection)) {
            if (MiningMode.skipMiningFor(player, level, pos, level.getBlockState(p))) continue;
            if (!original.call(instance, entity, p)) {
                return false;
            }
        }
        return true;
    }

    @WrapOperation(method = "destroyAndAck", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyBlock(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean handleMultiDestroy(ServerPlayerGameMode instance, BlockPos pos, Operation<Boolean> original) {
        this.currentlyDestroyed = pos;
        try {
            var tool = player.getMainHandItem();
            if (!tool.has(FactoryDataComponents.SELECTED_MINING_MODE)) {
                return original.call(instance, pos);
            }

            for (var p : tool.getOrDefault(FactoryDataComponents.SELECTED_MINING_MODE, MiningMode.SINGLE).positions(player, this.level, pos, level.getBlockState(pos), this.miningDirection)) {
                var head = tool.get(FactoryDataComponents.DRILL_ATTACHMENT);
                if (MiningMode.skipMiningFor(player, level, pos, level.getBlockState(p))) continue;
                original.call(instance, p);

                if (tool.get(FactoryDataComponents.DRILL_ATTACHMENT) != head || tool.isBroken()) return false;
            }
            return false;
        } finally {
            this.currentlyDestroyed = null;
        }
    }
}
