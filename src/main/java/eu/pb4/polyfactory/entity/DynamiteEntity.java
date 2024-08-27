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
            this.move(MovementType.SELF, this.getVelocity());
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

        if (this.stickToBlock == null) {
            HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
            if (hitResult.getType() != HitResult.Type.MISS) {
                this.hitOrDeflect(hitResult);
            }
        }
        this.checkBlockCollision();
        this.updateRotation();

        boolean move = true;
        if (this.stickToBlock != null) {
            move = false;

            var state = this.getWorld().getBlockState(this.stickToBlock);
            if (state.getCollisionShape(this.getWorld(), this.stickToBlock).isEmpty()) {
                this.stickToBlock = null;
            }
        }

        if (move) {
            this.move(MovementType.SELF, this.getVelocity());

            if (!this.hasNoGravity()) {
                this.setVelocity(this.getVelocity().add(0.0, -0.04, 0.0));
            }

            this.setVelocity(this.getVelocity().multiply(0.98));

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
            this.updateWaterState();
            world.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + getHeight(), this.getZ(), 0,0.0, 0.0, 0.0, 0);
        }
    }

    private void explode() {
        this.getWorld().createExplosion(this, this.getX(), this.getBodyY(0.5), this.getZ(), 2.6F, World.ExplosionSourceType.TNT);
    }

    @Override
    public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
        return EntityType.ITEM_DISPLAY;
    }

    @Override
    public void modifyRawTrackedData(List<DataTracker.SerializedEntry<?>> data, ServerPlayerEntity player, boolean initial) {
        if (initial) {
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.TELEPORTATION_DURATION, 2));
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.SCALE, new Vector3f(0.6f)));
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.BILLBOARD, (byte) DisplayEntity.BillboardMode.CENTER.ordinal()));
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.Item.ITEM, this.itemStack));
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
