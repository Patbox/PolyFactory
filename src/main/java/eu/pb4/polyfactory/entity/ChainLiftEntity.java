package eu.pb4.polyfactory.entity;

import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.mechanical.ChainDriveBlock;
import eu.pb4.polyfactory.block.mechanical.ChainDriveBlockEntity;
import eu.pb4.polyfactory.block.mechanical.RotationConstants;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.*;

import static eu.pb4.polyfactory.ModInit.id;

public class ChainLiftEntity extends VehicleEntity implements PolymerEntity {
    private static final ItemStack MODEL = ItemDisplayElementUtil.getModel(id("entity/chain_lift"));
    @Nullable
    private BlockPos sourcePos;
    @Nullable
    private BlockPos targetPos;

    @Nullable
    private Vec3d attachedPos;

    private float progress = 0;
    private float centerAngle = 0;
    private int ticksSinceBounceEffect = -1;
    private int timeInCenter = 0;

    private static final TrackedData<Quaternionf> ROTATION = DataTracker.registerData(ChainLiftEntity.class, TrackedDataHandlerRegistry.QUATERNION_F);

    public ChainLiftEntity(EntityType<?> entityType, World world) {
        super(entityType, world);
        EntityAttachment.ofTicking(new Model(), this);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ROTATION, new Quaternionf());
    }

    public static ActionResult attach(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var ent = new ChainLiftEntity(FactoryEntities.CHAIN_LIFT, world);

        var vec = hit.getPos().subtract(Vec3d.ofCenter(pos)).normalize().negate();

        var angle = (float) switch (state.get(ChainDriveBlock.AXIS)) {
            case Y -> Math.atan2(-vec.x, -vec.z);
            case X -> Math.atan2(vec.z, vec.y);
            case Z -> Math.atan2(-vec.x, vec.y);
        };
        var facingRotFrom = (switch (state.get(ChainDriveBlock.AXIS)) {
            case Y -> Direction.UP.getRotationQuaternion();
            case X -> Direction.EAST.getRotationQuaternion();
            case Z -> Direction.SOUTH.getRotationQuaternion();
        }).rotateY(angle);

        ent.attachedPos = Vec3d.ofCenter(pos).add(new Vec3d(new Vector3f(0, 0, ChainDriveBlock.RADIUS).rotate(facingRotFrom)));
        var entPos = Vec3d.ofCenter(pos).add(new Vec3d(new Vector3f(0, 0, ChainDriveBlock.RADIUS + 0.1f).rotate(facingRotFrom))).subtract(0, ent.getHeight(), 0);
        ent.setPosition(entPos);
        ent.sourcePos = pos;
        ent.centerAngle = angle;
        ent.setYaw(-angle * MathHelper.DEGREES_PER_RADIAN - 180);
        var box = new Box(entPos.x - 0.4, entPos.y, entPos.z - 0.4, entPos.x + 0.4, entPos.y + 1.5f, entPos.z + 0.4);

        if (!world.isSpaceEmpty(ent, box )) {
            return ActionResult.FAIL;
        }

        world.spawnEntity(ent);
        stack.decrementUnlessCreative(1, player);
        return ActionResult.SUCCESS_SERVER;
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        var firstPassenger = this.getFirstPassenger();
        if (firstPassenger != null && !(firstPassenger instanceof PlayerEntity)) {
            firstPassenger.dismountVehicle();
            return ActionResult.SUCCESS_SERVER;
        } else {
            player.startRiding(this);
            return ActionResult.SUCCESS_SERVER;
        }
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        return this.getPos();
    }

    @Override
    public void tick() {
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }

        var chainDrive = this.sourcePos != null && this.getWorld().getBlockEntity(this.sourcePos) instanceof ChainDriveBlockEntity be ? be : null;
        var route = this.targetPos != null && chainDrive != null ? chainDrive.getRoute(this.targetPos) : null;

        if (chainDrive == null || route == null && this.attachedPos != null && Vec3d.ofCenter(this.sourcePos).squaredDistanceTo(this.attachedPos) > (ChainDriveBlock.RADIUS * 1.5)) {
            this.targetPos = this.sourcePos = null;
            this.attachedPos = null;
            this.setVelocity(this.getVelocity().multiply(0.98).add(0, -0.06, 0));
            this.move(MovementType.SELF, this.getVelocity());
            super.tick();
            if (this.isOnGround()) {
                this.killAndDropSelf(world, world.getDamageSources().fall());
            }
            return;
        }
        if (this.attachedPos == null) {
            this.attachedPos = this.getPos().add(0, this.getHeight(), 0);
        }

        var passanger = this.getFirstPassenger();
        var rotation = new Quaternionf();

        if (world.isReceivingRedstonePower(this.getBlockPos())) {
            if (this.hasPassengers()) {
                this.removeAllPassengers();
            }
            rotation.rotateY(10 * MathHelper.RADIANS_PER_DEGREE * (this.random.nextBoolean() ? 1 : -1));

            passanger = null;
        } else if (passanger == null) {
            var pos = this.getPos();
            var box = new Box(pos.x - 0.4, pos.y, pos.z - 0.4, pos.x + 0.4, pos.y + 1, pos.z + 0.4);
            var ents = world.getOtherEntities(this, box, this::canPickupEntity);
            for (var ent : ents) {
                passanger = ent;
                if (passanger.startRiding(this, passanger instanceof AbstractMinecartEntity)) {
                    break;
                }
            }
        }

        Vec3d pos = this.getPos(), attachedPos = this.attachedPos;
        var rotMax = (RotationConstants.MAX_ROTATION_PER_TICK_4 - MathHelper.RADIANS_PER_DEGREE * 10) / ChainDriveBlock.RADIUS;

        var moveSpeed = Math.clamp(RotationUser.getRotation(world, this.sourcePos).speedRadians(), -rotMax, rotMax);
        var reverse = moveSpeed < 0;
        var yaw = this.getYaw();

        var centeredChainDrive = Vec3d.ofCenter(chainDrive.getPos());
        var canMove = this.ticksSinceBounceEffect % 2 != 0;
        var bounced = !canMove;

        if (route == null && canMove) {
            var axis = chainDrive.getCachedState().get(ChainDriveBlock.AXIS);
            var facingRotFrom = (switch (axis) {
                case Y -> Direction.UP.getRotationQuaternion();
                case X -> Direction.EAST.getRotationQuaternion();
                case Z -> Direction.SOUTH.getRotationQuaternion();
            }).rotateY(this.centerAngle);

            var offset = new Vec3d(new Vector3f(0, 0, ChainDriveBlock.RADIUS).rotate(facingRotFrom));
            pos = centeredChainDrive.subtract(0, this.getHeight(), 0).add(new Vec3d(new Vector3f(0, 0, ChainDriveBlock.RADIUS + 0.1f).rotate(facingRotFrom)));
            attachedPos = centeredChainDrive.add(offset);

            var moved = this.checkMove(world, this.getPos(), pos);
            pos = this.getPos().lerp(pos, moved);
            attachedPos = this.attachedPos.lerp(attachedPos, moved);

            bounced = moved != 1f;

            var newTarget = this.findNewTarget(chainDrive, offset, reverse, moveSpeed * moved);
            var angle = 0d;
            yaw = -this.centerAngle * MathHelper.DEGREES_PER_RADIAN - 90;
            this.timeInCenter++;

            if (newTarget != null) {
                if (reverse) {
                    this.targetPos = this.sourcePos;
                    this.sourcePos = newTarget.getLeft();
                    this.progress = (float) (newTarget.getRight().distance() - (float) ((moveSpeed - angle) * ChainDriveBlock.RADIUS * moved));
                } else {
                    this.targetPos = newTarget.getLeft();
                    this.progress = (float) ((moveSpeed - angle) * ChainDriveBlock.RADIUS * moved);
                }
                this.centerAngle = 0;
            } else {
                this.centerAngle = (this.centerAngle + moveSpeed * moved) % MathHelper.TAU;
            }
        } else if (route == null) {
            this.timeInCenter++;
        } else if (canMove) {
            this.timeInCenter = 0;
            attachedPos = centeredChainDrive.add(route.startPos().lerp(route.startOffset(), this.progress / route.distance()));
            pos = attachedPos.subtract(0, this.getHeight(), 0);
            var moved = this.checkMove(world, this.getPos(), pos);
            pos = this.getPos().lerp(pos, moved);
            attachedPos = this.attachedPos.lerp(attachedPos, moved);

            bounced = moved != 1f;
            var offset = route.startOffset().subtract(route.startPos()).normalize();
            yaw = (float) (MathHelper.atan2(offset.z, offset.x) * MathHelper.DEGREES_PER_RADIAN) - 90;

            var mappedProgress = reverse ? route.distance() - this.progress : progress;

            if (mappedProgress + Math.abs(moveSpeed * moved) * ChainDriveBlock.RADIUS >= route.distance()) {
                var chainDrive2 = reverse ? chainDrive : this.getWorld().getBlockEntity(this.targetPos) instanceof ChainDriveBlockEntity be ? be : null;
                if (chainDrive2 == null) {
                    this.sourcePos = null;
                    this.targetPos = null;
                    return;
                }

                var angle = 0f;
                var vec = reverse ? route.endPos() : Vec3d.ofCenter(this.targetPos)
                        .subtract(Vec3d.ofCenter(this.sourcePos))
                        .subtract(route.startOffset());


                angle = (float) switch (chainDrive2.getCachedState().get(ChainDriveBlock.AXIS)) {
                    case Y -> Math.atan2(-vec.x, -vec.z);
                    case X -> Math.atan2(vec.z, vec.y);
                    case Z -> Math.atan2(-vec.x, vec.y);
                };

                //var newTarget = this.findNewTarget(chainDrive2, vec, reverse, 0.01f);

                if (!reverse) {
                    this.sourcePos = this.targetPos;
                }

                this.progress = 0;
                this.centerAngle = angle;
                this.targetPos = null;//newTarget != null ? newTarget.getLeft() : null;
            } else {
                this.progress += moveSpeed * ChainDriveBlock.RADIUS * moved;
            }
        }

        this.setYaw(yaw);
        if (passanger != null) {
            if (passanger instanceof AbstractMinecartEntity) {
                passanger.setYaw(yaw - 90);
            } else if (!(passanger instanceof LivingEntity) || passanger instanceof ArmorStandEntity) {
                passanger.setYaw(yaw);
            }
        }


        if (bounced) {
            if (this.ticksSinceBounceEffect++ % 15 == 0) {
                this.playSound(SoundEvents.BLOCK_CHAIN_BREAK, 0.5f, 0.8f);
                world.spawnParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.3, pos.z, 5, 0.3, 0.3, 0.3 ,0);
            } else if (this.ticksSinceBounceEffect % 5 == 0) {
                rotation.rotateX(-this.random.nextTriangular(0.5f, 0.2f) * 20 * MathHelper.RADIANS_PER_DEGREE);
            }
        } else {
            this.ticksSinceBounceEffect = -1;
        }
        this.dataTracker.set(ROTATION, new Quaternionf(this.dataTracker.get(ROTATION)).mul(rotation).slerp(new Quaternionf(), 0.6f));

        if (!Objects.equals(this.attachedPos, attachedPos)) {
            this.attachedPos = attachedPos;
        }

        if (!this.getPos().equals(pos)) {
            this.setVelocity(pos.subtract(this.getPos()));
            this.setPosition(pos);
        }

        super.tick();
    }

    private float checkMove(ServerWorld world, Vec3d currentPos, Vec3d newPos) {
        var move = 1f;
        while (true) {
            var box = new Box(newPos.x - 0.2, newPos.y, newPos.z - 0.2, newPos.x + 0.2, newPos.y + 1.4f, newPos.z + 0.2);
            var entbox = new Box(newPos.x - 0.35, newPos.y, newPos.z - 0.35, newPos.x + 0.35, newPos.y + 1.4f, newPos.z + 0.35);

            if (world.isSpaceEmpty(this, box) && world.getOtherEntities(this, entbox,
                    x -> x instanceof ChainLiftEntity chainLift && x != this
                            && (this.targetPos != null ? this.progress < chainLift.progress : this.timeInCenter < chainLift.timeInCenter)).isEmpty()) {
                return move;
            }

            if (newPos.squaredDistanceTo(currentPos) < 0.005) {
                return 0;
            }
            newPos = currentPos.lerp(newPos, 0.95);
            move *= 0.95f;
        }
    }

    private boolean canPickupEntity(Entity entity) {
        return !(entity instanceof PlayerEntity)
                && entity.getHeight() < 2
                && !(entity instanceof ProjectileEntity)
                && !(entity instanceof Leashable leashable && leashable.isLeashed())
                && !entity.hasVehicle()
                ;
    }

    private Pair<BlockPos, ChainDriveBlock.Route> findNewTarget(ChainDriveBlockEntity chainDrive, Vec3d offset, boolean reverse, float moveSpeed) {
        Collection<BlockPos> conns;

        if (this.getFirstPassenger() instanceof ServerPlayerEntity player) {
            conns = new ArrayList<>();
            var camera = player.getRotationVector();
            var tmp = new ArrayList<Pair<BlockPos, Double>>();
            for (var conn : chainDrive.connections()) {
                var vec = Vec3d.ofCenter(conn).subtract(player.getEyePos()).normalize();
                tmp.add(new Pair<>(conn, vec.subtract(camera).lengthSquared()));
            }
            tmp.sort(Comparator.comparingDouble(Pair::getRight));
            if (!tmp.isEmpty() && tmp.getFirst().getRight() <= 0.25) {
                conns.add(tmp.getFirst().getLeft());
            } else {
                for (var t : tmp) {
                    conns.add(t.getLeft());
                }
            }
        } else {
            conns = chainDrive.connections();
        }

        for (var conn : conns) {
            if (conn.equals(this.sourcePos) || conn.equals(this.targetPos)) {
                continue;
            }

            var route = chainDrive.getRoute(conn);

            if (!reverse && route.startPos().distanceTo(offset) <= moveSpeed * ChainDriveBlock.RADIUS * 1.5) {
                return new Pair<>(conn, route);
            }
            if (reverse && route.endPos().distanceTo(offset) <= -moveSpeed * ChainDriveBlock.RADIUS * 1.5) {
                return new Pair<>(conn, route);
            }
        }

        return null;
    }

    @Override
    protected Item asItem() {
        return FactoryItems.CHAIN_LIFT;
    }

    @Override
    protected void readCustomData(ReadView view) {
        this.sourcePos = view.read("source_pos", BlockPos.CODEC).orElse(null);
        this.targetPos = view.read("target_pos", BlockPos.CODEC).orElse(null);
        this.centerAngle = view.getFloat("center_angle", 0f);
        this.progress = view.getFloat("progress", 0f);
        this.attachedPos = view.read("attached_pos", Vec3d.CODEC).orElse(null);
        this.timeInCenter = view.getInt("time_in_center", 0);
    }

    @Override
    protected void writeCustomData(WriteView view) {
        view.putNullable("source_pos", BlockPos.CODEC, this.sourcePos);
        view.putNullable("attached_pos", Vec3d.CODEC, this.attachedPos);

        if (this.targetPos != null) {
            view.put("target_pos", BlockPos.CODEC, this.targetPos);
            view.putFloat("progress", this.progress);
        } else if (this.sourcePos != null) {
            view.putFloat("center_angle", this.centerAngle);
            view.putInt("time_in_center", this.timeInCenter);
        }
    }

    @Override
    public void modifyRawTrackedData(List<DataTracker.SerializedEntry<?>> data, ServerPlayerEntity player, boolean initial) {
        var interpolate = false;
        for (int i = 0; i < data.size(); i++) {
            var entry = data.get(i);
            if (entry.id() == ROTATION.id()) {
                data.set(i, DataTracker.SerializedEntry.of(DisplayTrackedData.LEFT_ROTATION, (Quaternionf) entry.value()));
                interpolate = true;
            }
        }
        if (initial) {
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.TELEPORTATION_DURATION, 3));
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.INTERPOLATION_DURATION, 2));
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.TRANSLATION, new Vector3f(0, 13 / 16f, 0)));
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.Item.ITEM, MODEL));
        } else if (interpolate) {
            data.add(DataTracker.SerializedEntry.of(DisplayTrackedData.START_INTERPOLATION, 0));
        }

        PolymerEntity.super.modifyRawTrackedData(data, player, initial);
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext packetContext) {
        return EntityType.ITEM_DISPLAY;
    }

    private class Model extends ElementHolder {
        public final InteractionElement interaction;
        private boolean noTick = true;

        public Model() {
            var interaction = VirtualElement.InteractionHandler.redirect(ChainLiftEntity.this);
            this.interaction = new InteractionElement(interaction);
            this.interaction.setSendPositionUpdates(false);
            this.addPassengerElement(this.interaction);
        }

        @Override
        public boolean startWatching(ServerPlayNetworkHandler player) {
            if (noTick) {
                onTick();
            }
            return super.startWatching(player);
        }

        @Override
        protected void onTick() {
            noTick = false;
            this.interaction.setCustomName(ChainLiftEntity.this.getCustomName());
            this.interaction.setCustomNameVisible(ChainLiftEntity.this.isCustomNameVisible());
            if (ChainLiftEntity.this.hasPlayerRider()) {
                this.interaction.setSize(0, ChainLiftEntity.this.getHeight() - 0.1f);
            } else if (ChainLiftEntity.this.hasPassengers()) {
                var passanger = ChainLiftEntity.this.getFirstPassenger();
                this.interaction.setSize(Math.max(passanger.getWidth() + 0.1f, ChainLiftEntity.this.getWidth() + 0.1f),
                        Math.clamp(passanger.getHeight() - 0.1f, 0.2f, 0.5f));
            } else {
                this.interaction.setSize(ChainLiftEntity.this.getWidth() + 0.1f, 0.5f);
            }

            //MAT.translate(0.0F, -0.2f, 0.0F);
            //MAT.translate(0.0F, -1.501F, 0.0F);

            //MAT.rotateY(ChainLiftEntity.this.getYaw() * MathHelper.RADIANS_PER_DEGREE - MathHelper.PI / 2);
            //this.model.update(ChainLiftEntity.this, MAT);

            super.onTick();
        }
    }
}
