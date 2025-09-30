package eu.pb4.polyfactory.mixin.player;

import com.mojang.authlib.GameProfile;
import eu.pb4.factorytools.api.util.FactoryPlayer;
import eu.pb4.polyfactory.block.mechanical.ChainDriveBlock;
import eu.pb4.polyfactory.util.LastFanEffectedTickConsumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements LastFanEffectedTickConsumer, ChainDriveBlock.ChainDriveHandler {
    @Shadow public abstract ServerWorld getEntityWorld();

    @Unique
    private BlockPos chainStart;

    @Unique
    private int lastFanAgeOnGround = -9999;

    @Unique
    private double lastFanY = -9999;

    public ServerPlayerEntityMixin(World world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void clearChainStart(CallbackInfo ci) {
        if (this.chainStart == null) {
            return;
        }

        if (!this.getMainHandStack().isOf(Items.IRON_CHAIN) && !this.getOffHandStack().isOf(Items.IRON_CHAIN)) {
            this.chainStart = null;
            return;
        }

        var raycast = this.raycast(this.getBlockInteractionRange(), 0, false);
        if (raycast instanceof BlockHitResult result) {
            ChainDriveBlock.showPreview((ServerPlayerEntity) (Object) this, this.getEntityWorld(), this.chainStart, result.getBlockPos());
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

    @Override
    public BlockPos polyfactory$getCurrentChainStart() {
        return this.chainStart;
    }

    @Override
    public void polyfactory$setCurrentChainStart(BlockPos pos) {
        this.chainStart = pos;
    }
}
