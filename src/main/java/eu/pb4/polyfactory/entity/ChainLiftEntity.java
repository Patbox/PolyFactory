package eu.pb4.polyfactory.entity;

import com.mojang.serialization.Codec;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.mechanical.ChainDriveBlock;
import eu.pb4.polyfactory.block.mechanical.ChainDriveBlockEntity;
import eu.pb4.polyfactory.block.mechanical.RotationConstants;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.entity.configurable.*;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.*;
import java.util.function.Consumer;

import static eu.pb4.polyfactory.ModInit.id;

public class ChainLiftEntity extends VehicleEntity implements PolymerEntity, ConfigurableEntity<ChainLiftEntity>, EntityCatchingVehicle {
    private static final ItemStack MODEL = ItemDisplayElementUtil.getSolidModel(id("entity/chain_lift"));
    private static final EntityConfig<Boolean, ChainLiftEntity> PLAYER_CONTROL_CONFIG = EntityConfig.of("player_control", Codec.BOOL,
            EntityConfigValue.of(x -> x.playerControllable, (x, y) -> x.playerControllable = y),
            EntityValueFormatter.text(CommonComponents::optionStatus),
            WrenchModifyEntityValue.iterate(List.of(false, true))
    );

    private static final List<EntityConfig<?, ChainLiftEntity>> CONFIGURATION = List.of(
            PLAYER_CONTROL_CONFIG,
            CATCH_ENTITIES_CONFIG.cast(),
            EntityConfig.DISMOUNT.cast()
    );

    @Nullable
    private BlockPos sourcePos;
    @Nullable
    private BlockPos targetPos;

    @Nullable
    private Vec3 attachedPos;

    private float progress = 0;
    private float centerAngle = 0;
    private int ticksSinceBounceEffect = -1;
    private int timeInCenter = 0;

    private int bounceAnimationTimes = -1;
    private int shakeAnimationTimer = -1;
    private boolean canCatchEntities = true;
    private boolean playerControllable = true;

    private static final EntityDataAccessor<Quaternionfc> ROTATION = SynchedEntityData.defineId(ChainLiftEntity.class, EntityDataSerializers.QUATERNION);

    public ChainLiftEntity(EntityType<?> entityType, Level world) {
        super(entityType, world);
        EntityAttachment.ofTicking(new Model(), this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROTATION, new Quaternionf());
    }

    public static InteractionResult attach(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var ent = new ChainLiftEntity(FactoryEntities.CHAIN_LIFT, world);
        ent.applyComponentsFromItemStack(stack);

        var vec = hit.getLocation().subtract(Vec3.atCenterOf(pos)).normalize().reverse();

        var angle = calculateAngle(vec, state.getValue(ChainDriveBlock.AXIS));
        var facingRotFrom = (switch (state.getValue(ChainDriveBlock.AXIS)) {
            case Y -> Direction.UP.getRotation();
            case X -> Direction.EAST.getRotation();
            case Z -> Direction.SOUTH.getRotation();
        }).rotateY(angle);

        ent.attachedPos = Vec3.atCenterOf(pos).add(new Vec3(new Vector3f(0, 0, ChainDriveBlock.RADIUS).rotate(facingRotFrom)));
        var entPos = Vec3.atCenterOf(pos).add(new Vec3(new Vector3f(0, 0, ChainDriveBlock.RADIUS + 0.1f).rotate(facingRotFrom)))
                .subtract(0, ent.getType().getHeight(), 0);
        ent.setPos(entPos);
        ent.sourcePos = pos;
        ent.centerAngle = angle;
        ent.setYRot(-angle * Mth.RAD_TO_DEG - 90);
        var box = new AABB(entPos.x - 0.4, entPos.y, entPos.z - 0.4, entPos.x + 0.4, entPos.y + 1.5f, entPos.z + 0.4);

        if (!world.noCollision(ent, box )) {
            return InteractionResult.FAIL;
        }

        world.addFreshEntity(ent);
        stack.consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.isVehicle() && !this.hasExactlyOnePlayerPassenger()) {
            this.ejectPassengers();
            this.shakeAnimationTimer = 10;
            return InteractionResult.SUCCESS_SERVER;
        } else {
            player.startRiding(this);
            return InteractionResult.SUCCESS_SERVER;
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return this.position();
    }

    @Override
    public void tick() {
        if (!(this.level() instanceof ServerLevel world)) {
            return;
        }

        var chainDrive = this.sourcePos != null && this.level().getBlockEntity(this.sourcePos) instanceof ChainDriveBlockEntity be ? be : null;
        var route = this.targetPos != null && chainDrive != null ? chainDrive.getRoute(this.targetPos) : null;
        //noinspection deprecation
        this.fixupDimensions();
        if (chainDrive == null || route == null && this.attachedPos != null && Vec3.atCenterOf(this.sourcePos).distanceToSqr(this.attachedPos) > (ChainDriveBlock.RADIUS * 2)) {
            this.targetPos = this.sourcePos = null;
            this.attachedPos = null;
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98).add(0, -0.06, 0));
            this.move(MoverType.SELF, this.getDeltaMovement());
            super.tick();
            if (this.onGround()) {
                this.destroy(world, world.damageSources().fall());
            }
            return;
        }
        if (this.attachedPos == null) {
            this.attachedPos = this.position().add(0, this.getType().getHeight(), 0);
        }

        var passanger = this.getFirstPassenger();

        if (world.hasNeighborSignal(this.blockPosition()) || world.hasNeighborSignal(this.blockPosition().above())) {
            if (this.isVehicle()) {
                this.ejectPassengers();
            }
            this.shakeAnimationTimer = 10;

            passanger = null;
        } else if (passanger == null && this.canCatchEntities) {
            var pos = this.position();
            var box = new AABB(pos.x - 0.4, pos.y, pos.z - 0.4, pos.x + 0.4, pos.y + 1, pos.z + 0.4);
            var ents = world.getEntities(this, box, this::canPickupEntity);
            for (var ent : ents) {
                passanger = ent;
                if (passanger.startRiding(this, passanger instanceof AbstractMinecart, true)) {
                    break;
                }
                passanger = null;
            }
        }

        Vec3 pos = this.position(), attachedPos = this.attachedPos;
        var rotMax = (RotationConstants.MAX_ROTATION_PER_TICK_4 - Mth.DEG_TO_RAD * 10) / ChainDriveBlock.RADIUS;

        var moveSpeed = Math.clamp(RotationUser.getRotation(world, this.sourcePos).speedRadians(), -rotMax, rotMax);
        var reverse = moveSpeed < 0;
        var yaw = this.getYRot();

        var centeredChainDrive = Vec3.atCenterOf(chainDrive.getBlockPos());
        var canMove = this.ticksSinceBounceEffect == -1 || this.ticksSinceBounceEffect > 4;
        var bounced = !canMove;

        if (route == null && canMove) {
            var axis = chainDrive.getBlockState().getValue(ChainDriveBlock.AXIS);
            var facingRotFrom = (switch (axis) {
                case Y -> Direction.UP.getRotation();
                case X -> Direction.EAST.getRotation();
                case Z -> Direction.SOUTH.getRotation();
            }).rotateY(this.centerAngle);

            var offset = new Vec3(new Vector3f(0, 0, ChainDriveBlock.RADIUS).rotate(facingRotFrom));
            pos = centeredChainDrive.subtract(0, this.getType().getHeight(), 0).add(new Vec3(new Vector3f(0, 0, ChainDriveBlock.RADIUS + 0.1f).rotate(facingRotFrom)));
            attachedPos = centeredChainDrive.add(offset);

            var moved = this.checkMove(world, this.position(), pos);
            pos = this.position().lerp(pos, moved);
            attachedPos = this.attachedPos.lerp(attachedPos, moved);

            bounced = moved != 1f;

            var newTarget = this.findNewTarget(chainDrive, offset, reverse, moveSpeed * moved);
            var angle = 0d;
            yaw = -this.centerAngle * Mth.RAD_TO_DEG - 90;
            this.timeInCenter++;

            if (newTarget != null) {
                if (reverse) {
                    this.targetPos = this.sourcePos;
                    this.sourcePos = newTarget.getA();
                    this.progress = (float) (newTarget.getB().distance() - (float) ((moveSpeed - angle) * ChainDriveBlock.RADIUS * moved));
                } else {
                    this.targetPos = newTarget.getA();
                    this.progress = (float) ((moveSpeed - angle) * ChainDriveBlock.RADIUS * moved);
                }
                this.centerAngle = 0;
            } else {
                this.centerAngle = (this.centerAngle + moveSpeed * moved) % Mth.TWO_PI;
            }
        } else if (route == null) {
            this.timeInCenter++;
        } else if (canMove) {
            this.timeInCenter = 0;
            attachedPos = centeredChainDrive.add(route.startPos().lerp(route.startOffset(), this.progress / route.distance()));
            pos = attachedPos.subtract(0, this.getType().getHeight(), 0);
            var moved = this.checkMove(world, this.position(), pos);
            pos = this.position().lerp(pos, moved);
            attachedPos = this.attachedPos.lerp(attachedPos, moved);

            bounced = moved != 1f;
            var offset = route.startOffset().subtract(route.startPos()).normalize();
            yaw = (float) (Mth.atan2(offset.z, offset.x) * Mth.RAD_TO_DEG) - 90;

            var mappedProgress = reverse ? route.distance() - this.progress : progress;

            if (mappedProgress + Math.abs(moveSpeed * moved) * ChainDriveBlock.RADIUS >= route.distance() - ChainDriveBlock.RADIUS) {
                var chainDrive2 = reverse ? chainDrive : this.level().getBlockEntity(this.targetPos) instanceof ChainDriveBlockEntity be ? be : null;
                if (chainDrive2 == null) {
                    this.sourcePos = null;
                    this.targetPos = null;
                    return;
                }

                var angle = 0f;
                var vec = Vec3.atCenterOf(reverse ? this.sourcePos : this.targetPos).subtract(attachedPos);


                angle = calculateAngle(vec, chainDrive2.getBlockState().getValue(ChainDriveBlock.AXIS));

                var newTarget = this.findNewTarget(chainDrive2, vec, reverse, (float) ((mappedProgress + Math.abs(moveSpeed * moved) - route.distance() + ChainDriveBlock.RADIUS) * Mth.sign(moveSpeed)));

                if (!reverse) {
                    this.sourcePos = this.targetPos;
                }

                this.progress = 0;
                this.centerAngle = angle;
                this.targetPos = newTarget != null ? newTarget.getA() : null;
            } else {
                this.progress += moveSpeed * ChainDriveBlock.RADIUS * moved;
            }
        }

        this.setYRot(Mth.rotLerp(0.3f, this.getYRot(), yaw));
        if (passanger != null) {
            if (passanger instanceof AbstractMinecart) {
                passanger.setYRot(this.getYRot() - 90);
            } else if (!(passanger instanceof LivingEntity) || passanger instanceof ArmorStand) {
                passanger.setYRot(this.getYRot());
            }
        }


        if (bounced) {
            if (this.ticksSinceBounceEffect++ % 15 == 0) {
                this.playSound(SoundEvents.CHAIN_BREAK, 0.5f, 0.8f);
                world.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.3, pos.z, 5, 0.3, 0.3, 0.3 ,0);
            } else if (this.ticksSinceBounceEffect % 5 == 0) {
                this.bounceAnimationTimes = 8;
            }
        } else {
            this.ticksSinceBounceEffect = -1;
        }

        var rotation = new Quaternionf();

        if (this.shakeAnimationTimer > 0) {
            rotation.rotateY((float) (Math.sin((this.tickCount % 360) * Mth.DEG_TO_RAD * 50) * (this.shakeAnimationTimer / 32f)));
            this.shakeAnimationTimer--;
        }
        if (this.bounceAnimationTimes > 0) {
            rotation.rotateX(-this.bounceAnimationTimes * 1.5f * Mth.DEG_TO_RAD);
            this.bounceAnimationTimes--;
        }
        this.entityData.set(ROTATION, rotation);

        if (!Objects.equals(this.attachedPos, attachedPos)) {
            this.attachedPos = attachedPos;
        }

        if (!this.position().equals(pos)) {
            this.setDeltaMovement(pos.subtract(this.position()));
            this.setPos(pos);
            if (passanger instanceof ServerPlayer serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.CHAIN_LIFT);
            }
        } else {
            this.reapplyPosition();
        }

        super.tick();
    }

    private float checkMove(ServerLevel world, Vec3 currentPos, Vec3 newPos) {
        var move = 1f;
        while (true) {
            var box = new AABB(newPos.x - 0.2, newPos.y + 0.1, newPos.z - 0.2, newPos.x + 0.2, newPos.y + 1.4f, newPos.z + 0.2);
            var entbox = new AABB(newPos.x - 0.35, newPos.y, newPos.z - 0.35, newPos.x + 0.35, newPos.y + 1.4f, newPos.z + 0.35);

            var finalNewPos = newPos;
            if (world.noCollision(this, box) && world.getEntities(this, entbox,
                    x -> {
                        if (!(x instanceof ChainLiftEntity chainLift)) {
                            return false;
                        }

                        if (chainLift.position().equals(finalNewPos)) {
                            return this.getId() < chainLift.getId();
                        } else if (this.targetPos != null && this.targetPos.equals(chainLift.targetPos)) {
                            return this.progress < chainLift.progress;
                        } else {
                            return this.timeInCenter < chainLift.timeInCenter;
                        }
                    }).isEmpty()) {
                return move;
            }

            if (newPos.distanceToSqr(currentPos) < 0.005) {
                return 0;
            }
            newPos = currentPos.lerp(newPos, 0.95);
            move *= 0.95f;
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        var dim = super.getDimensions(pose);
        float width, height;
        if (this.isVehicle()) {
            var passanger = this.getFirstPassenger();

            width = Math.max(passanger.getBbWidth() + 0.1f, dim.width() + 0.1f);
            height = Math.clamp(passanger.getBbHeight() - 0.1f, 0.2f, 0.5f);
        } else {
            width = dim.width() + 0.1f;
            height = 0.5f;
        }

        return new EntityDimensions(width, height, height * 0.75f, dim.attachments(), false);
    }

    @Override
    public boolean getRequiresPrecisePosition() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    private boolean canPickupEntity(Entity entity) {
        return !(entity instanceof Player)
                && !(entity instanceof ChainLiftEntity)
                && entity.getBbHeight() < 2
                && !(entity instanceof Projectile)
                && !(entity instanceof Leashable leashable && leashable.isLeashed())
                && !entity.isPassenger()
                ;
    }
    private static float calculateAngle(Vec3 vec, Direction.Axis axis) {
        return (float) switch (axis) {
            case Y -> Math.atan2(-vec.x, -vec.z);
            case X -> Math.atan2(vec.z, vec.y);
            case Z -> Math.atan2(-vec.x, vec.y);
        };
    }

    private Tuple<BlockPos, ChainDriveBlock.Route> findNewTarget(ChainDriveBlockEntity chainDrive, Vec3 offset, boolean reverse, float moveSpeed) {
        List<BlockPos> conns;

        if (this.getFirstPassenger() instanceof ServerPlayer player && this.playerControllable) {
            conns = new ArrayList<>();
            var camera = player.getLookAngle();
            var tmp = new ArrayList<Tuple<BlockPos, Double>>();
            for (var conn : chainDrive.connections()) {
                var vec = Vec3.atCenterOf(conn).subtract(player.getEyePosition()).normalize();
                tmp.add(new Tuple<>(conn, vec.subtract(camera).lengthSqr()));
            }
            tmp.sort(Comparator.comparingDouble(Tuple::getB));
            if (!tmp.isEmpty() && tmp.getFirst().getB() <= 0.25) {
                conns.add(tmp.getFirst().getA());
            } else {
                for (var t : tmp) {
                    conns.add(t.getA());
                }
            }
        } else {
            conns = new ArrayList<>(chainDrive.connections());
            conns.sort(Comparator.comparingDouble(conn -> {
                var route = chainDrive.getRoute(conn);
                var shift = new Vec3(new Vector3f(ChainDriveBlock.RADIUS - 0.3f, 0, 0).rotate(route.facingRotTo()));

                if (reverse) {
                    return Math.abs(route.endPos().add(shift).distanceTo(offset));
                }

                return Math.abs(route.startPos().add(shift).distanceTo(offset));
            }));
        }
        //var tmp = offset.add(Vec3d.ofCenter(this.sourcePos));
        //((ServerWorld) this.getEntityWorld()).spawnParticles(ParticleTypes.BUBBLE, tmp.x, tmp.y + 2, tmp.z, 0, 0, 0, 0, 0);

        for (var conn : conns) {
            if (conn.equals(this.sourcePos) || conn.equals(this.targetPos)) {
                continue;
            }

            var route = chainDrive.getRoute(conn);
            var shift = new Vec3(new Vector3f(ChainDriveBlock.RADIUS - 0.3f, 0, 0).rotate(route.facingRotTo()));
            //tmp = route.startPos().add(shift).add(Vec3d.ofCenter(this.sourcePos));
            //((ServerWorld) this.getEntityWorld()).spawnParticles(ParticleTypes.BUBBLE, tmp.x, tmp.y + 1, tmp.z, 0, 0, 0, 0, 0);
            if (!reverse && route.startPos().add(shift).distanceTo(offset) <= Math.max(moveSpeed, 0.3f)) {
                return new Tuple<>(conn, route);
            }
            if (reverse && route.endPos().add(shift).distanceTo(offset) <= Math.max(-moveSpeed, 0.3f)) {
                return new Tuple<>(conn, route);
            }
        }

        return null;
    }

    @Override
    public void setHurtTime(int damageWobbleTicks) {
        super.setHurtTime(damageWobbleTicks);
        this.shakeAnimationTimer = damageWobbleTicks;
    }

    @Override
    protected Item getDropItem() {
        return FactoryItems.CHAIN_LIFT;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return getDropItem().getDefaultInstance();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        this.sourcePos = view.read("source_pos", BlockPos.CODEC).orElse(null);
        this.targetPos = view.read("target_pos", BlockPos.CODEC).orElse(null);
        this.centerAngle = view.getFloatOr("center_angle", 0f);
        this.progress = view.getFloatOr("progress", 0f);
        this.attachedPos = view.read("attached_pos", Vec3.CODEC).orElse(null);
        this.timeInCenter = view.getIntOr("time_in_center", 0);
        this.canCatchEntities = view.getBooleanOr("can_catch_entities", true);
        this.playerControllable = view.getBooleanOr("player_control", true);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        view.storeNullable("source_pos", BlockPos.CODEC, this.sourcePos);
        view.storeNullable("attached_pos", Vec3.CODEC, this.attachedPos);

        if (this.targetPos != null) {
            view.store("target_pos", BlockPos.CODEC, this.targetPos);
            view.putFloat("progress", this.progress);
        } else if (this.sourcePos != null) {
            view.putFloat("center_angle", this.centerAngle);
            view.putInt("time_in_center", this.timeInCenter);
        }

        view.putBoolean("can_catch_entities", this.canCatchEntities);
        view.putBoolean("player_control", this.playerControllable);
    }

    @Override
    public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
        var interpolate = false;
        for (int i = 0; i < data.size(); i++) {
            var entry = data.get(i);
            if (entry.id() == ROTATION.id()) {
                data.set(i, SynchedEntityData.DataValue.create(DisplayTrackedData.LEFT_ROTATION, (Quaternionf) entry.value()));
                interpolate = true;
            }
        }
        if (initial) {
            data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.TELEPORTATION_DURATION, 3));
            data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.INTERPOLATION_DURATION, 2));
            data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.TRANSLATION, new Vector3f(0, 13 / 16f, 0)));
            data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.Item.ITEM, MODEL));
        } else if (interpolate) {
            data.add(SynchedEntityData.DataValue.create(DisplayTrackedData.START_INTERPOLATION, 0));
        }

        PolymerEntity.super.modifyRawTrackedData(data, player, initial);
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext packetContext) {
        return EntityType.ITEM_DISPLAY;
    }

    @Override
    public List<EntityConfig<?, ChainLiftEntity>> getEntityConfiguration(ServerPlayer player, Vec3 targetPos) {
        return CONFIGURATION;
    }

    @Override
    public void writeMinimalConfigurationToStack(Consumer<EntityConfig<?, ChainLiftEntity>> consumer) {
        if (!this.canCatchEntities) {
            consumer.accept(CATCH_ENTITIES_CONFIG.cast());
        }
        if (!this.playerControllable) {
            consumer.accept(PLAYER_CONTROL_CONFIG);
        }
    }

    @Override
    public boolean polyfactory$canCatchEntities() {
        return this.canCatchEntities;
    }

    @Override
    public void polyfactory$setCatchEntities(boolean value) {
        this.canCatchEntities = value;
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
        public boolean startWatching(ServerGamePacketListenerImpl player) {
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

            if (ChainLiftEntity.this.hasExactlyOnePlayerPassenger()) {
                this.interaction.setSize(0, ChainLiftEntity.this.getBbHeight() - 0.1f);
            } else {
                this.interaction.setSize(ChainLiftEntity.this.getBbWidth(), ChainLiftEntity.this.getBbHeight());
            }

            //MAT.translate(0.0F, -0.2f, 0.0F);
            //MAT.translate(0.0F, -1.501F, 0.0F);

            //MAT.rotateY(ChainLiftEntity.this.getYaw() * MathHelper.RADIANS_PER_DEGREE - MathHelper.PI / 2);
            //this.model.update(ChainLiftEntity.this, MAT);

            super.onTick();
        }
    }
}
