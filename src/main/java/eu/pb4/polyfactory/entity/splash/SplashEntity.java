package eu.pb4.polyfactory.entity.splash;

import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.polyfactory.entity.FluidDataOwner;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.mixin.ProjectileAccessor;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public abstract class SplashEntity<T> extends Projectile implements PolymerEntity, FluidDataOwner<T> {
    private final FluidType<T> fluid;
    private T data;
    protected int existenceTime = 5;
    @Nullable
    private GameProfile profile;
    public SplashEntity(EntityType<? extends Projectile> entityType, Level world, FluidType<T> fluidInstance) {
        super(entityType, world);
        this.fluid = fluidInstance;
        this.data = fluidInstance.defaultData();
    }



    @Override
    public void setFluidData(T data) {
        this.data = data;
    }

    @Override
    public T getFluidData() {
        return this.data;
    }

    @Override
    public FluidType<T> fluidType() {
        return this.fluid;
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.polyfactory.splash", this.fluid.getName(this.getFluidData()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        if (this.data != this.fluid.defaultData()) {
            view.store("fluid_data", this.fluid.dataCodec(), this.data);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        this.data = view.read("fluid_data", this.fluid.dataCodec()).orElse(this.fluid.defaultData());
    }

    public ParticleOptions getBaseParticle() {
        return this.fluid.toInstance(this.data).particle();
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (hitResult.getType() != HitResult.Type.MISS && !(hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == this.getOwner())) {
            this.spawnDestructionParticles(hitResult.getLocation());
            this.discard();
        }
    }

    protected void spawnDestructionParticles(Vec3 pos) {
        ((ServerLevel) this.level()).sendParticles(this.getBaseParticle(), pos.x, pos.y, pos.z,
                5, 0, 0, 0, this.getParticleCollisionSpeed());
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (entityHitResult.getEntity() == this.getOwner() || !this.canInteractEntity(entityHitResult.getEntity())) {
            return;
        }
        var velocity = this.getDeltaMovement().scale(0.004);
        FactoryUtil.addSafeVelocity(entityHitResult.getEntity(), velocity);
        if (entityHitResult.getEntity() instanceof ServerPlayer player) {
            FactoryUtil.sendVelocityDelta(player, velocity);
        }
    }
    @Override
    public void tick() {
        super.tick();

        if (this.discardInBlock(this.getInBlockState(), this.blockPosition())) {
            this.discard();
        }

        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.hitTargetOrDeflectSelf(hitResult);
        }
        //this.checkBlockCollision();
        this.updateRotation();

        if (this.isAlive()) {
            this.spawnExistenceParticles();
            this.move(MoverType.SELF, this.getDeltaMovement());
        }
        if (this.tickCount > this.existenceTime) {
            this.onNaturalDiscard();
            this.discard();
            return;
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.99));
        this.applyGravity();
    }

    protected void onNaturalDiscard() {
    }

    @Override
    public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
        PolymerEntity.super.modifyRawTrackedData(data, player, initial);
        if (initial) {
            data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.TELEPORTATION_DURATION, 2));
            if (!this.forceParticles()) {
                data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.BILLBOARD, (byte) Display.BillboardConstraints.CENTER.ordinal()));
                if (this.fluid.brightness().isPresent()) {
                    data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.BRIGHTNESS, this.fluid.brightness().get().pack()));
                }
                data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.Item.ITEM, getParticleItem()));
            }
        }
    }

    protected ItemStack getParticleItem() {
        return FactoryModels.FLUID_PARTICLE.get(this.fluid, this.data);
    }

    protected boolean forceParticles() {
        return true;
    }


    protected void spawnExistenceParticles() {
        if (forceParticles() && this.tickCount % 2 == 1) {
            var velocity = this.getDeltaMovement();
            ((ServerLevel) this.level()).sendParticles(this.getBaseParticle(), this.getX(), this.getY(), this.getZ(),
                    0, velocity.x(), velocity.y(), velocity.z(), this.getParticleSpeed());
        }
    }
    protected boolean discardInBlock(BlockState state, BlockPos blockPos) {
        if (state.getFluidState().is(FluidTags.LAVA)) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(),
                    0, 0, 0, 0, 0);
            this.playEntityOnFireExtinguishedSound();
        }

        return !state.getFluidState().isEmpty();
    }

    protected double getParticleSpeed() {
        return 0.48;
    }

    protected double getParticleCollisionSpeed() {
        return 0.005;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.01;
    }

    @Override
    public boolean canUsePortal(boolean allowVehicles) {
        return true;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof SplashEntity<?>);
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext player) {
        return EntityType.ITEM_DISPLAY;
    }

    @Override
    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.EMPTY;
    }

    @Override
    protected SoundEvent getSwimHighSpeedSplashSound() {
        return SoundEvents.EMPTY;
    }

    protected GameProfile getProfile() {
        if (this.getOwner() instanceof Player player) {
            this.profile = player.getGameProfile();
        } else if (this.profile == null) {
            var x = ((ProjectileAccessor) this).getOwner();
            this.profile = new GameProfile(x != null ? x.getUUID() : this.getUUID(), "Splash");
        }
        return this.profile;
    }

    protected boolean canDamageEntity(Entity entity) {
        return CommonProtection.canDamageEntity(level(), entity, getProfile(), this.getOwner() instanceof Player player ? player : null);
    }

    protected boolean canInteractEntity(Entity entity) {
        return CommonProtection.canInteractEntity(level(), entity, getProfile(), this.getOwner() instanceof Player player ? player : null);
    }

    protected boolean canBreakBlock(BlockPos pos) {
        return CommonProtection.canBreakBlock(level(), pos, getProfile(), this.getOwner() instanceof Player player ? player : null);
    }

    protected boolean canPlaceBlock(BlockPos pos) {
        return CommonProtection.canPlaceBlock(level(), pos, getProfile(), this.getOwner() instanceof Player player ? player : null);
    }
}
