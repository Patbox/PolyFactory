package eu.pb4.polyfactory.entity.splash;

import com.mojang.datafixers.util.Pair;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public abstract class SplashEntity<T> extends ProjectileEntity implements PolymerEntity {
    private final FluidType<T> fluid;
    private T data;
    protected int existenceTime = 5;
    public SplashEntity(EntityType<? extends ProjectileEntity> entityType, World world, FluidType<T> fluidInstance) {
        super(entityType, world);
        this.fluid = fluidInstance;
        this.data = fluidInstance.defaultData();
    }

    public void setData(T data) {
        this.data = data;
    }

    public T getData() {
        return this.data;
    }

    @Override
    protected Text getDefaultName() {
        return Text.translatable("entity.polyfactory.splash", this.fluid.getName(this.getData()));
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
            ((ServerWorld) this.getWorld()).spawnParticles(this.getBaseParticle(), hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z,
                    5, 0, 0, 0, this.getParticleCollisionSpeed());
            this.discard();
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        if (entityHitResult.getEntity() == this.getOwner()) {
            return;
        }
        var velocity = this.getVelocity().multiply(0.008);
        FactoryUtil.addSafeVelocity(entityHitResult.getEntity(), velocity);
        if (entityHitResult.getEntity() instanceof ServerPlayerEntity player) {
            FactoryUtil.sendVelocityDelta(player, velocity);
        }
    }

    @Override
    public void tick() {
        super.tick();
        var velocity = this.getVelocity();
        HitResult hitResult = ProjectileUtil.getCollision(this, this::canHitX);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.hitOrDeflect(hitResult);
        }
        this.checkBlockCollision();
        this.updateRotation();

        if (this.isAlive()) {
            if (this.age % 2 == 1) {
                ((ServerWorld) this.getWorld()).spawnParticles(this.getBaseParticle(), this.getX(), this.getY(), this.getZ(),
                        0, velocity.getX(), velocity.getY(), velocity.getZ(), this.getParticleSpeed());
            }

            this.move(MovementType.SELF, velocity);
        }
        if (this.age > this.existenceTime) {
            this.discard();
            return;
        }
        this.setVelocity(this.getVelocity().multiply(0.99));
        this.applyGravity();
    }

    protected double getParticleSpeed() {
        return 0.12;
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

    protected boolean canHitX(Entity entity) {
        return this.canHit(entity);
    }

    @Override
    protected boolean canHit(Entity entity) {
        return super.canHit(entity) && !(entity instanceof SplashEntity<?>);
    }

    @Override
    public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
        return EntityType.ITEM_DISPLAY;
    }
}
