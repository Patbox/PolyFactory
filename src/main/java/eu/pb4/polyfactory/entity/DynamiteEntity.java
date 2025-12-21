package eu.pb4.polyfactory.entity;

import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class DynamiteEntity extends Projectile implements PolymerEntity {

    private int fuse = 80;
    private ItemStack itemStack = FactoryItems.DYNAMITE.getDefaultInstance();
    private BlockPos stickToBlock;

    private Vec3 particlePos = null;

    public DynamiteEntity(EntityType<? extends Projectile> type, Level world) {
        super(type, world);
    }

    public DynamiteEntity(Level world, @Nullable LivingEntity igniter) {
        this(FactoryEntities.DYNAMITE, world);
        this.setOwner(igniter);
    }

    public static void spawn(Vec3 vector, Position pos, Level world, ItemStack stack, @Nullable LivingEntity igniter) {
        var entity = create(vector, pos, world, stack, igniter);
        world.addFreshEntity(entity);
    }

    public static DynamiteEntity create(Vec3 vector, Position pos, Level world, ItemStack stack, @Nullable LivingEntity igniter) {
        var entity = new DynamiteEntity(world, igniter);
        entity.setItemStack(stack);
        entity.setPos(new Vec3(pos.x(), pos.y(), pos.z()));
        entity.setDeltaMovement(vector);
        return entity;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemStack itemStack() {
        return itemStack;
    }

    @Override
    protected void onHitBlock(BlockHitResult blockHitResult) {
        if (this.itemStack.is(FactoryItems.STICKY_DYNAMITE)) {
            this.stickToBlock = blockHitResult.getBlockPos();
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        var axis = blockHitResult.getDirection().getAxis();
        this.setDeltaMovement(this.getDeltaMovement().multiply(axis == Direction.Axis.X ? -0.25 : 0.25, blockHitResult.getDirection() == Direction.DOWN ? -1 : 1, axis == Direction.Axis.Z ? -0.25 : 0.25));
    }

    public void tick() {
        if (!(this.level() instanceof ServerLevel world)) {
            return;
        }
        super.tick();
        this.updateRotation();

        if (this.stickToBlock != null) {
            var state = this.level().getBlockState(this.stickToBlock);
            if (state.getCollisionShape(this.level(), this.stickToBlock).isEmpty()) {
                this.stickToBlock = null;
            }
        }
        if (this.tickCount == 6 && this.getOwner() instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityDataPacket(this.getId(),
                    List.of(SynchedEntityData.DataValue.create(DisplayTrackedData.Item.ITEM, this.itemStack))));
        }

        if (this.stickToBlock == null) {
            if (!this.isNoGravity()) {
                this.applyGravity();
            }

            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));

            HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            this.setPos(hitResult.getLocation().subtract(this.getDeltaMovement().normalize().scale(0.05f)));
            this.applyEffectsFromBlocks();
            if (hitResult.getType() != HitResult.Type.MISS) {
                this.hitTargetOrDeflectSelf(hitResult);
            }

            if (this.onGround()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, -0.8, 0.8));
            }

            if (this.isInWater()) {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
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
            if (!this.level().isClientSide()) {
                this.explode();
            }
        } else {
            if (this.particlePos == null) {
                this.particlePos = new Vec3(this.getX(), this.getY() + getBbHeight(), this.getZ());
            }
            this.particlePos = this.particlePos.lerp(this.position().add(0, getBbHeight(), 0),  1 / 5f);

            this.updateInWaterStateAndDoFluidPushing();
            world.sendParticles(ParticleTypes.SMOKE, this.particlePos.x, this.particlePos.y, this.particlePos.z, 0, 0.0, 0.0, 0.0, 0);
        }
    }

    private void explode() {
        this.level().explode(this, this.getX(), this.getY(0.5), this.getZ(), 2.6F, Level.ExplosionInteraction.TNT);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04f;
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityType.ITEM_DISPLAY;
    }

    @Override
    public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
        if (initial) {
            data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.TELEPORTATION_DURATION, 3));
            data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.SCALE, new Vector3f(0.6f)));
            data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.BILLBOARD, (byte) Display.BillboardConstraints.CENTER.ordinal()));
            data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.Item.ITEM, this.tickCount < 4 && this.ownedBy(player) ? ItemStack.EMPTY : this.itemStack));
            data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.Item.ITEM_DISPLAY, ItemDisplayContext.FIXED.getId()));
        }
    }

    @Override
    public Vec3 trackingPosition() {
        return super.trackingPosition().add(0, this.getBbHeight() / 2, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        this.fuse = view.getShortOr("fuse", (short) 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        view.putShort("fuse", (short) this.fuse);
    }
}
