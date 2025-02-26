package eu.pb4.polyfactory.entity;

import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.List;

public class DynamiteEntity extends ProjectileEntity implements PolymerEntity {

    private int fuse = 80;
    private ItemStack itemStack = FactoryItems.DYNAMITE.getDefaultStack();
    private BlockPos stickToBlock;

    private Vec3d particlePos = null;

    public DynamiteEntity(EntityType<? extends ProjectileEntity> type, World world) {
        super(type, world);
    }

    public static void spawn(Vec3d vector, Position pos, World world, ItemStack stack) {
        var entity = create(vector, pos, world, stack);
        world.spawnEntity(entity);
    }

    public static DynamiteEntity create(Vec3d vector, Position pos, World world, ItemStack stack) {
        var entity = new DynamiteEntity(FactoryEntities.DYNAMITE, world);
        entity.setItemStack(stack);
        entity.setPosition(new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
        entity.setVelocity(vector);
        return entity;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemStack itemStack() {
        return itemStack;
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (this.itemStack.isOf(FactoryItems.STICKY_DYNAMITE)) {
            this.stickToBlock = blockHitResult.getBlockPos();
            this.setVelocity(Vec3d.ZERO);
            return;
        }

        var axis = blockHitResult.getSide().getAxis();
        this.setVelocity(this.getVelocity().multiply(axis == Direction.Axis.X ? -0.25 : 0.25, blockHitResult.getSide() == Direction.DOWN ? -1 : 1, axis == Direction.Axis.Z ? -0.25 : 0.25));
    }

    public void tick() {
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        super.tick();
        this.updateRotation();

        if (this.stickToBlock != null) {
            var state = this.getWorld().getBlockState(this.stickToBlock);
            if (state.getCollisionShape(this.getWorld(), this.stickToBlock).isEmpty()) {
                this.stickToBlock = null;
            }
        }
        if (this.age == 6 && this.getOwner() instanceof ServerPlayerEntity player) {
            player.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(this.getId(),
                    List.of(DataTracker.SerializedEntry.of(DisplayTrackedData.Item.ITEM, this.itemStack))));
        }

        if (this.stickToBlock == null) {
            if (!this.hasNoGravity()) {
                this.applyGravity();
            }

            this.setVelocity(this.getVelocity().multiply(0.98));

            HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
            this.setPosition(hitResult.getPos().subtract(this.getVelocity().normalize().multiply(0.05f)));
            this.tickBlockCollision();
            if (hitResult.getType() != HitResult.Type.MISS) {
                this.hitOrDeflect(hitResult);
            }

            if (this.isOnGround()) {
                this.setVelocity(this.getVelocity().multiply(0.8, -0.8, 0.8));
            }

            if (this.isTouchingWater()) {
                this.setVelocity(this.getVelocity().multiply(0.8));
            }
        }

        if (this.isOnFire()) {
            this.discard();
            this.explode();
        }

        int i = this.fuse - 1;
        this.fuse = i;
        if (i <= 0) {
            this.discard();
            if (!this.getWorld().isClient) {
                this.explode();
            }
        } else {
            if (this.particlePos == null) {
                this.particlePos = new Vec3d(this.getX(), this.getY() + getHeight(), this.getZ());
            }
            this.particlePos = this.particlePos.lerp(this.getPos().add(0, getHeight(), 0),  1 / 5f);

            this.updateWaterState();
            world.spawnParticles(ParticleTypes.SMOKE, this.particlePos.x, this.particlePos.y, this.particlePos.z, 0, 0.0, 0.0, 0.0, 0);
        }
    }

    private void explode() {
        this.getWorld().createExplosion(this, this.getX(), this.getBodyY(0.5), this.getZ(), 2.6F, World.ExplosionSourceType.TNT);
    }

    @Override
    protected double getGravity() {
        return 0.04f;
    }

    @Override
    public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
        return EntityType.ITEM_DISPLAY;
    }

    @Override
    public void modifyRawTrackedData(List<DataTracker.SerializedEntry<?>> data, ServerPlayerEntity player, boolean initial) {
        if (initial) {
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.TELEPORTATION_DURATION, 3));
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.SCALE, new Vector3f(0.6f)));
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.BILLBOARD, (byte) DisplayEntity.BillboardMode.CENTER.ordinal()));
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.Item.ITEM, this.age < 4 && this.isOwner(player) ? ItemStack.EMPTY : this.itemStack));
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.Item.ITEM_DISPLAY, ModelTransformationMode.FIXED.getIndex()));
        }
    }

    @Override
    public Vec3d getClientSidePosition(Vec3d vec3d) {
        return vec3d.add(0, this.getHeight() / 2, 0);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {

    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.fuse = nbt.getShort("fuse");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putShort("fuse", (short) this.fuse);
    }
}
