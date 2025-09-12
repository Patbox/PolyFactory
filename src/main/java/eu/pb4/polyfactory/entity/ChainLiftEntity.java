package eu.pb4.polyfactory.entity;

import eu.pb4.polyfactory.block.mechanical.ChainDriveBlock;
import eu.pb4.polyfactory.block.mechanical.ChainDriveBlockEntity;
import eu.pb4.polyfactory.block.mechanical.RotationConstants;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class ChainLiftEntity extends VehicleEntity implements PolymerEntity {
    @Nullable
    private BlockPos sourcePos;
    @Nullable
    private BlockPos targetPos;

    private float progress = 0;
    private float centerAngle = 0;


    public ChainLiftEntity(EntityType<?> entityType, World world) {
        super(entityType, world);
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

        var shift = 0.45f;
        var offset = new Vec3d(new Vector3f(0, 0, shift).rotate(facingRotFrom));
        ent.setPosition(Vec3d.ofCenter(pos).add(offset).subtract(0, ent.getHeight(), 0));
        ent.sourcePos = pos;
        ent.centerAngle = angle;
        ent.setYaw(-angle * MathHelper.DEGREES_PER_RADIAN - 180);
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
    public void tick() {
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }

        var chainDrive = this.sourcePos != null && this.getWorld().getBlockEntity(this.sourcePos) instanceof ChainDriveBlockEntity be ? be : null;
        var route = this.targetPos != null && chainDrive != null ? chainDrive.getRoute(this.targetPos) : null;

        if (chainDrive == null || route == null && Vec3d.ofCenter(this.sourcePos).squaredDistanceTo(this.getPos().add(0, this.getHeight(), 0)) > 0.5) {
            this.targetPos = this.sourcePos = null;
            this.setVelocity(this.getVelocity().multiply(0.98).add(0, -0.06, 0));
            this.move(MovementType.SELF, this.getVelocity());
            super.tick();
            if (this.isOnGround()) {
                this.killAndDropSelf(world, world.getDamageSources().fall());
            }
            return;
        }

        Vec3d pos;
        var shift = 0.45f;
        var rotMax = (RotationConstants.MAX_ROTATION_PER_TICK_4 - MathHelper.RADIANS_PER_DEGREE * 10) / 0.45f;

        var moveSpeed = Math.clamp(RotationUser.getRotation(world, this.sourcePos).speedRadians(), -rotMax, rotMax);
        var reverse = moveSpeed < 0;

        if (route == null) {
            var axis = chainDrive.getCachedState().get(ChainDriveBlock.AXIS);
            var facingRotFrom = (switch (axis) {
                case Y -> Direction.UP.getRotationQuaternion();
                case X -> Direction.EAST.getRotationQuaternion();
                case Z -> Direction.SOUTH.getRotationQuaternion();
            }).rotateY(this.centerAngle);

            var offset = new Vec3d(new Vector3f(0, 0, shift).rotate(facingRotFrom));
            pos = Vec3d.ofCenter(chainDrive.getPos()).add(offset);
            var newTarget = this.findNewTarget(chainDrive, offset, reverse, moveSpeed);
            var angle = 0d;

            if (newTarget != null) {
                if (reverse) {
                    this.targetPos = this.sourcePos;
                    this.sourcePos = newTarget.getLeft();
                    this.progress = (float) (newTarget.getRight().distance() - (float) ((moveSpeed - angle) * shift));
                } else {
                    this.targetPos = newTarget.getLeft();
                    this.progress = (float) ((moveSpeed - angle) * shift);
                }
                this.centerAngle = 0;
            } else {
                this.centerAngle = (this.centerAngle + moveSpeed) % MathHelper.TAU;
            }
        } else {
            pos = Vec3d.ofCenter(chainDrive.getPos()).add(route.startPos().lerp(route.startOffset(), this.progress / route.distance()));

            var mappedProgress = reverse ? route.distance() - this.progress : progress;

            if (mappedProgress + Math.abs(moveSpeed) * shift >= route.distance()) {
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
                this.progress += moveSpeed * shift;
            }
        }

        pos = pos.subtract(0, this.getHeight(), 0);
        if (!this.getPos().equals(pos)) {
            this.lookAt(EntityAnchorArgumentType.EntityAnchor.FEET, pos);
            this.setYaw(this.getYaw() - 90);
            this.setVelocity(pos.subtract(this.getPos()));
            this.setPosition(pos);
        }

        super.tick();
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

            if (!reverse && route.startPos().distanceTo(offset) <= moveSpeed * 0.8f) {
                return new Pair<>(conn, route);
            }
            if (reverse && route.endPos().distanceTo(offset) <= -moveSpeed * 0.8f) {
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
    }

    @Override
    protected void writeCustomData(WriteView view) {
        view.putNullable("source_pos", BlockPos.CODEC, this.sourcePos);

        if (this.targetPos != null) {
            view.put("target_pos", BlockPos.CODEC, this.targetPos);
            view.putFloat("progress", this.progress);
        } else if (this.sourcePos != null) {
            view.putFloat("center_angle", this.centerAngle);
        }
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext packetContext) {
        return EntityType.MINECART;
    }
}
