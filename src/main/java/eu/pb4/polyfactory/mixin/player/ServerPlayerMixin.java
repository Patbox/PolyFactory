package eu.pb4.polyfactory.mixin.player;

import com.mojang.authlib.GameProfile;
import eu.pb4.polyfactory.block.mechanical.ChainDriveBlock;
import eu.pb4.polyfactory.util.LastFanEffectedTickConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements LastFanEffectedTickConsumer, ChainDriveBlock.ChainDriveHandler {
    @Shadow public abstract ServerLevel level();

    @Unique
    private BlockPos chainStart;

    @Unique
    private int lastFanAgeOnGround = -9999;

    @Unique
    private double lastFanY = -9999;

    public ServerPlayerMixin(Level world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void clearChainStart(CallbackInfo ci) {
        if (this.chainStart == null) {
            return;
        }

        if (!this.getMainHandItem().is(Items.IRON_CHAIN) && !this.getOffhandItem().is(Items.IRON_CHAIN)) {
            this.chainStart = null;
            return;
        }

        var raycast = this.pick(this.blockInteractionRange(), 0, false);
        if (raycast instanceof BlockHitResult result) {
            ChainDriveBlock.showPreview((ServerPlayer) (Object) this, this.level(), this.chainStart, result.getBlockPos());
        }
    }

    @Override
    public void polyfactory$setLastFanTick() {
        //this.lastFanAge = this.age;
        if (this.onGround()) {
            this.lastFanAgeOnGround = this.tickCount;
            this.lastFanY = this.getY();
        }
    }

    @Override
    public double polyfactory$getLastY() {
        return this.lastFanY;
    }

    @Override
    public int polyfactory$getLastOnGround() {
        return this.lastFanAgeOnGround;
    }

    @Override
    public BlockPos polyfactory$getCurrentChainStart() {
        return this.chainStart;
    }

    @Override
    public void polyfactory$setCurrentChainStart(BlockPos pos) {
        this.chainStart = pos;
    }
}
