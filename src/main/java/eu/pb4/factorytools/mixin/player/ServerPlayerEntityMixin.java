package eu.pb4.factorytools.mixin.player;

import com.mojang.authlib.GameProfile;
import eu.pb4.factorytools.api.util.FactoryPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity {

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
}
