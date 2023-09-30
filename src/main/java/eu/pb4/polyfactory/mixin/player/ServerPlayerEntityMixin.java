package eu.pb4.polyfactory.mixin.player;

import com.mojang.authlib.GameProfile;
import eu.pb4.polyfactory.util.FactoryPlayer;
import eu.pb4.polyfactory.util.LastFanEffectedTickConsumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements LastFanEffectedTickConsumer {
    @Shadow public abstract ServerStatHandler getStatHandler();

    //@Unique
    //private int lastFanAge = -9999;

    @Unique
    private int lastFanAgeOnGround = -9999;

    @Unique
    private double lastFanY = -9999;

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @SuppressWarnings("ConstantValue")
    @Inject(method = "moveToSpawn", at = @At("HEAD"), cancellable = true)
    private void dontMoveFakePlayers(ServerWorld world, CallbackInfo ci) {
        if (((Object) this) instanceof FactoryPlayer) {
            ci.cancel();
        }
    }

    @Override
    public void polyfactory$setLastFanTick() {
        //this.lastFanAge = this.age;
        if (this.isOnGround()) {
            this.lastFanAgeOnGround = this.age;
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
}
