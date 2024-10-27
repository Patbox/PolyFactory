package eu.pb4.polyfactory.entity.splash;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.polyfactory.entity.FluidDataOwner;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.mixin.ProjectileEntityAccessor;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public abstract class SplashEntity<T> extends ProjectileEntity implements PolymerEntity, FluidDataOwner<T> {
    private final FluidType<T> fluid;
    private T data;
    protected int existenceTime = 5;
    @Nullable
    private GameProfile profile;
    public SplashEntity(EntityType<? extends ProjectileEntity> entityType, World world, FluidType<T> fluidInstance) {
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
    protected Text getDefaultName() {
        return Text.translatable("entity.polyfactory.splash", this.fluid.getName(this.getFluidData()));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.data != this.fluid.defaultData()) {
            nbt.put("fluid_data", this.fluid.dataCodec().encodeStart(this.getRegistryManager().getOps(NbtOps.INSTANCE), this.data).getOrThrow());
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("fluid_data")) {
            this.data = this.fluid.dataCodec().decode(this.getRegistryManager().getOps(NbtOps.INSTANCE), nbt.get("fluid_data")).result().map(Pair::getFirst).orElse(fluid.defaultData());
        } else {
            this.data = this.fluid.defaultData();
        }
    }

    public ParticleEffect getBaseParticle() {
        return this.fluid.toInstance(this.data).particle();
    }


    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (hitResult.getType() != HitResult.Type.MISS && !(hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == this.getOwner())) {
            this.spawnDestructionParticles(hitResult.getPos());
            this.discard();
        }
    }

    protected void spawnDestructionParticles(Vec3d pos) {
        ((ServerWorld) this.getWorld()).spawnParticles(this.getBaseParticle(), pos.x, pos.y, pos.z,
                5, 0, 0, 0, this.getParticleCollisionSpeed());
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (entityHitResult.getEntity() == this.getOwner() || !this.canInteractEntity(entityHitResult.getEntity())) {
            return;
        }
        var velocity = this.getVelocity().multiply(0.004);
        FactoryUtil.addSafeVelocity(entityHitResult.getEntity(), velocity);
        if (entityHitResult.getEntity() instanceof ServerPlayerEntity player) {
            FactoryUtil.sendVelocityDelta(player, velocity);
        }
    }
    @Override
    public void tick() {
        super.tick();

        if (this.discardInBlock(this.getBlockStateAtPos(), this.getBlockPos())) {
            this.discard();
        }

        HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.hitOrDeflect(hitResult);
        }
        //this.checkBlockCollision();
        this.updateRotation();

        if (this.isAlive()) {
            this.spawnExistenceParticles();
            this.move(MovementType.SELF, this.getVelocity());
        }
        if (this.age > this.existenceTime) {
            this.onNaturalDiscard();
            this.discard();
            return;
        }
        this.setVelocity(this.getVelocity().multiply(0.99));
        this.applyGravity();
    }

    protected void onNaturalDiscard() {
    }

    @Override
    public void modifyRawTrackedData(List<DataTracker.SerializedEntry<?>> data, ServerPlayerEntity player, boolean initial) {
        PolymerEntity.super.modifyRawTrackedData(data, player, initial);
        if (initial) {
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.TELEPORTATION_DURATION, 2));
            if (!this.forceParticles()) {
                data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.BILLBOARD, (byte) DisplayEntity.BillboardMode.CENTER.ordinal()));
                if (this.fluid.brightness().isPresent()) {
                    data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.BRIGHTNESS, this.fluid.brightness().get().pack()));
                }
                data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.Item.ITEM, getParticleItem()));
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
        if (forceParticles() && this.age % 2 == 1) {
            var velocity = this.getVelocity();
            ((ServerWorld) this.getWorld()).spawnParticles(this.getBaseParticle(), this.getX(), this.getY(), this.getZ(),
                    0, velocity.getX(), velocity.getY(), velocity.getZ(), this.getParticleSpeed());
        }
    }
    protected boolean discardInBlock(BlockState state, BlockPos blockPos) {
        if (state.getFluidState().isIn(FluidTags.LAVA)) {
            ((ServerWorld) this.getWorld()).spawnParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(),
                    0, 0, 0, 0, 0);
            this.playExtinguishSound();
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
    protected double getGravity() {
        return 0.01;
    }

    @Override
    public boolean canUsePortals(boolean allowVehicles) {
        return true;
    }

    @Override
    protected boolean canHit(Entity entity) {
        return super.canHit(entity) && !(entity instanceof SplashEntity<?>);
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext player) {
        return EntityType.ITEM_DISPLAY;
    }

    @Override
    protected SoundEvent getSplashSound() {
        return SoundEvents.INTENTIONALLY_EMPTY;
    }

    @Override
    protected SoundEvent getHighSpeedSplashSound() {
        return SoundEvents.INTENTIONALLY_EMPTY;
    }

    protected GameProfile getProfile() {
        if (this.getOwner() instanceof PlayerEntity player) {
            this.profile = player.getGameProfile();
        } else if (this.profile == null) {
            var x = ((ProjectileEntityAccessor) this).getOwnerUuid();
            this.profile = new GameProfile(x != null ? x : this.getUuid(), "Splash");
        }
        return this.profile;
    }

    protected boolean canDamageEntity(Entity entity) {
        return CommonProtection.canDamageEntity(getWorld(), entity, getProfile(), this.getOwner() instanceof PlayerEntity player ? player : null);
    }

    protected boolean canInteractEntity(Entity entity) {
        return CommonProtection.canInteractEntity(getWorld(), entity, getProfile(), this.getOwner() instanceof PlayerEntity player ? player : null);
    }

    protected boolean canBreakBlock(BlockPos pos) {
        return CommonProtection.canBreakBlock(getWorld(), pos, getProfile(), this.getOwner() instanceof PlayerEntity player ? player : null);
    }

    protected boolean canPlaceBlock(BlockPos pos) {
        return CommonProtection.canPlaceBlock(getWorld(), pos, getProfile(), this.getOwner() instanceof PlayerEntity player ? player : null);
    }
}
