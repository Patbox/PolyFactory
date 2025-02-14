package eu.pb4.polyfactory.block.collection;

import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.elements.AbstractElement;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import eu.pb4.polymer.virtualentity.api.tracker.EntityTrackedData;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.CollisionView;
import net.minecraft.world.border.WorldBorder;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;

public class BlockCollection extends AbstractElement implements CollisionView {
    private final DisplayElement main = ItemDisplayElementUtil.createSimple(ItemStack.EMPTY, 4);
    private final int[] blockId;
    private final int[] collisionBlockId;
    private final int[] collisionBlockId2;
    private final IntList blockIdList = new IntArrayList();
    private final IntList allIdList = new IntArrayList();
    private final BlockState[] states;
    private final BlockEntity[] blockEntities;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    private float centerX;
    private float centerY;
    private float centerZ;

    @Nullable
    private ServerWorld world;

    private final Quaternionf newQuaternion = Direction.UP.getRotationQuaternion();
    private final Quaternionf quaternion = Direction.UP.getRotationQuaternion();
    private boolean quaternionDirty = false;

    public BlockCollection(int x, int y, int z) {
        var size = x * y * z;
        this.states = new BlockState[size];
        this.blockEntities = new BlockEntity[size];
        this.blockId = new int[size];
        this.collisionBlockId = new int[size];
        this.collisionBlockId2 = new int[size];
        Arrays.fill(this.blockId, -1);
        Arrays.fill(this.collisionBlockId, -1);
        Arrays.fill(this.collisionBlockId2, -1);
        this.sizeX = x;
        this.sizeY = y;
        this.sizeZ = z;
        this.allIdList.add(this.main.getEntityId());
    }

    public void setCenter(float x, float y, float z) {
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
    }

    @Override
    public void startWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
        this.main.startWatching(player, packetConsumer);
        var pos = this.main.getCurrentPos();
        var vec = new Vector3f();
        var quaternion = new Quaternionf(this.quaternion);
        for (var x = 0; x < this.sizeX; x++) {
            for (var y = 0; y < this.sizeY; y++) {
                for (var z = 0; z < this.sizeZ; z++) {
                    sendInitialBlockVisual(packetConsumer, x, y, z, pos, vec, quaternion);
                    sendInitialBlockCollision(packetConsumer, x, y, z, pos, vec, quaternion);
                }
            }
        }

        packetConsumer.accept(VirtualEntityUtils.createRidePacket(this.main.getEntityId(), this.blockIdList));
    }


    public void sendInitialBlockVisual(Consumer<Packet<ClientPlayPacketListener>> packetConsumer, int x, int y, int z, Vec3d pos, Vector3f vec, Quaternionf quaternion) {
        var i = index(x, y, z);
        var id = this.blockId[i];
        if (id != -1) {
            packetConsumer.accept(new EntitySpawnS2CPacket(id, UUID.randomUUID(),
                    pos.x, pos.y, pos.z, 0f, 0f,
                    EntityType.BLOCK_DISPLAY, 0, Vec3d.ZERO, 0d
            ));

            packetConsumer.accept(new EntityTrackerUpdateS2CPacket(id, List.of(
                    DataTracker.SerializedEntry.of(DisplayTrackedData.INTERPOLATION_DURATION, 1),
                    DataTracker.SerializedEntry.of(DisplayTrackedData.Block.BLOCK_STATE, this.states[i]),
                    DataTracker.SerializedEntry.of(DisplayTrackedData.TRANSLATION, new Vector3f(x - 0.5f - this.centerX, y - 0.5f - this.centerY, z - 0.5f - this.centerZ)
                            .rotate(quaternion)),
                    DataTracker.SerializedEntry.of(DisplayTrackedData.LEFT_ROTATION, quaternion),
                    DataTracker.SerializedEntry.of(EntityTrackedData.FLAGS, (byte) (1 << EntityTrackedData.INVISIBLE_FLAG_INDEX))

            )));
        }
    }
    public void sendInitialBlockCollision(Consumer<Packet<ClientPlayPacketListener>> packetConsumer, int x, int y, int z, Vec3d pos, Vector3f vec, Quaternionf quaternion) {
        var i = index(x, y, z);
        var id = this.collisionBlockId[i];
        if (id != -1) {
            vec.set(x - this.centerX, y - this.centerY, z - this.centerZ).rotate(quaternion);
            packetConsumer.accept(new EntitySpawnS2CPacket(id, UUID.randomUUID(),
                    pos.x + vec.x, pos.y + vec.y - 0.5, pos.z + vec.z , 0f, 0f,
                    EntityType.SHULKER, 0, Vec3d.ZERO, 0d
            ));

            packetConsumer.accept(new EntityTrackerUpdateS2CPacket(id, List.of(
                    DataTracker.SerializedEntry.of(EntityTrackedData.FLAGS, (byte) (1 << EntityTrackedData.INVISIBLE_FLAG_INDEX))
            )));

            packetConsumer.accept(new EntitySpawnS2CPacket(this.collisionBlockId2[i], UUID.randomUUID(),
                    pos.x + vec.x, pos.y + vec.y - 0.5, pos.z + vec.z, 0f, 0f,
                    EntityType.BLOCK_DISPLAY, 0, Vec3d.ZERO, 0d
            ));

            packetConsumer.accept(new EntityTrackerUpdateS2CPacket(this.collisionBlockId2[i], List.of(
                    DataTracker.SerializedEntry.of(DisplayTrackedData.INTERPOLATION_DURATION, 1),
                    DataTracker.SerializedEntry.of(EntityTrackedData.FLAGS, (byte) (1 << EntityTrackedData.INVISIBLE_FLAG_INDEX))
            )));

            packetConsumer.accept(VirtualEntityUtils.createRidePacket(this.collisionBlockId2[i], IntList.of(id)));
        }
    }

    public void setQuaternion(Quaternionf quaternion) {
        this.newQuaternion.set(quaternion);
        this.quaternionDirty = true;
    }

    public void setBlockState(int x, int y, int z, BlockState state, @Nullable BlockEntity blockEntity) {
        var i = index(x, y, z);
        if (this.states[i] == state) {
            this.blockEntities[i] = blockEntity;
            return;
        }
        this.states[i] = state;
        this.blockEntities[i] = blockEntity;
        if (this.blockId[i] == -1 && !state.isAir()) {
            var e = VirtualEntityUtils.requestEntityId();
            this.blockId[i] = e;
            this.blockIdList.add(e);
            this.allIdList.add(e);

            var pos = this.main.getCurrentPos();
            var vec = new Vector3f();
            var quaternion = new Quaternionf(this.quaternion);
            if (this.getHolder() != null) {
                this.sendInitialBlockVisual(this.getHolder()::sendPacket, x, y, z, pos, vec, quaternion);
            }

            if (!state.getCollisionShape(this, new BlockPos(x, y, z)).isEmpty()) {
                var e2 = VirtualEntityUtils.requestEntityId();
                var e3 = VirtualEntityUtils.requestEntityId();
                this.collisionBlockId[i] = e2;
                this.collisionBlockId2[i] = e3;
                this.allIdList.add(e2);
                this.allIdList.add(e3);
                if (this.getHolder() != null) {
                    this.sendInitialBlockCollision(this.getHolder()::sendPacket, x, y, z, pos, vec, quaternion);
                }
            }
        } else if (state.isAir()) {
            this.blockIdList.removeInt(this.blockId[i]);
            this.allIdList.removeInt(this.blockId[i]);
            if (this.collisionBlockId[i] != -1) {
                this.allIdList.removeInt(this.collisionBlockId[i]);
                this.allIdList.removeInt(this.collisionBlockId2[i]);
                if (this.getHolder() != null) {
                    this.getHolder().sendPacket(new EntitiesDestroyS2CPacket(IntList.of(
                            this.blockId[i], this.collisionBlockId[i], this.collisionBlockId2[i])));
                }
            } else if (this.getHolder() != null) {
                this.getHolder().sendPacket(new EntitiesDestroyS2CPacket(IntList.of(this.blockId[i])));
            }
            this.blockId[i] = this.collisionBlockId[i] = this.collisionBlockId2[i] = -1;
        } else {
            var noCollision = state.getCollisionShape(this, new BlockPos(x, y, z)).isEmpty();
            if (this.getHolder() != null) {
                this.getHolder().sendPacket(new EntityTrackerUpdateS2CPacket(this.blockId[i], List.of(
                        DataTracker.SerializedEntry.of(DisplayTrackedData.Block.BLOCK_STATE, this.states[i])

                )));
            }

            if (this.collisionBlockId[i] != -1 && noCollision) {
                this.allIdList.removeInt(this.collisionBlockId[i]);
                this.allIdList.removeInt(this.collisionBlockId2[i]);
                if (this.getHolder() != null) {
                    this.getHolder().sendPacket(new EntitiesDestroyS2CPacket(IntList.of(this.collisionBlockId[i], this.collisionBlockId2[i])));
                }
                this.collisionBlockId[i] = this.collisionBlockId2[i] = 0;
            } else if (this.collisionBlockId[i] == -1 && !noCollision) {
                var e2 = VirtualEntityUtils.requestEntityId();
                var e3 = VirtualEntityUtils.requestEntityId();
                this.collisionBlockId[i] = e2;
                this.collisionBlockId2[i] = e3;
                this.allIdList.add(e2);
                this.allIdList.add(e3);
                if (this.getHolder() != null) {
                    var pos = this.main.getCurrentPos();
                    var vec = new Vector3f();
                    var quaternion = new Quaternionf(this.quaternion);
                    this.sendInitialBlockCollision(this.getHolder()::sendPacket, x, y, z, pos, vec, quaternion);
                }
            }
        }
    }
    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        var i = index(pos.getX(), pos.getY(), pos.getZ());
        return i != -1 ? this.blockEntities[i] : null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockState getBlockState(int x, int y, int z) {
        var i = index(x, y, z);
        return i != -1 ? this.states[i] : Blocks.VOID_AIR.getDefaultState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    private int index(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < this.sizeX && y < this.sizeY && z < this.sizeZ ? (x + y * this.sizeX) * this.sizeZ + z : -1;
    }

    @Override
    public int getHeight() {
        return this.sizeY;
    }
    @Override
    public int getBottomY() {
        return 0;
    }

    @Override
    public IntList getEntityIds() {
        return this.allIdList;
    }

    @Override
    public void stopWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
        this.main.stopWatching(player, packetConsumer);
    }

    @Override
    public void notifyMove(Vec3d oldPos, Vec3d currentPos, Vec3d delta) {
        this.main.notifyMove(oldPos, currentPos, delta);
    }

    @Override
    public void tick() {
        if (this.quaternionDirty) {
            var pos = this.main.getCurrentPos();
            var vec = new Vector3f();
            var vec2 = new Vector3f();
            var b = new ArrayList<Packet<? super ClientPlayPacketListener>>();
            var box = new Box(pos.x - this.centerX - 2, pos.y - this.centerY - 2, pos.z - this.centerZ - 2,
                    pos.x - this.centerX + this.sizeX + 2, pos.y - this.centerY + this.sizeY + 2, pos.z - this.centerZ + this.sizeZ + 2);

            var ents = this.world != null ? this.world.getOtherEntities(null, box, EntityPredicates.EXCEPT_SPECTATOR) : List.<Entity>of();
            var quaternion = new Quaternionf(this.newQuaternion);

            for (var x = 0; x < this.sizeX; x++) {
                for (var y = 0; y < this.sizeY; y++) {
                    for (var z = 0; z < this.sizeZ; z++) {
                        var i = index(x, y, z);
                        var id = this.blockId[i];
                        if (id != -1) {
                            b.add(new EntityTrackerUpdateS2CPacket(id, List.of(
                                    DataTracker.SerializedEntry.of(DisplayTrackedData.TRANSLATION, new Vector3f(x - 0.5f - this.centerX, y - 0.5f - this.centerY, z - 0.5f - this.centerZ).rotate(quaternion)),
                                    DataTracker.SerializedEntry.of(DisplayTrackedData.LEFT_ROTATION, quaternion),
                                    DataTracker.SerializedEntry.of(DisplayTrackedData.START_INTERPOLATION, 0)
                            )));
                        }
                        id = this.collisionBlockId2[i];
                        if (id != -1) {
                            vec.set(x - this.centerX, y - this.centerY, z - this.centerZ);
                            vec.rotate(this.newQuaternion);
                            vec2.set(x - this.centerX, y - this.centerY, z - this.centerZ);
                            vec2.rotate(this.quaternion);

                            for (var entity : ents) {
                                if (entity.getBoundingBox().intersects(
                                        pos.x + vec.x - 0.5, pos.y + vec.y - 0.5, pos.z + vec.z - 0.5,
                                        pos.x + vec.x + 0.5, pos.y + vec.y + 0.51, pos.z + vec.z + 0.5
                                )) {
                                    var delta = new Vec3d(vec.x - vec2.x, vec.y - vec2.y, vec.z - vec2.z);
                                    entity.move(MovementType.SHULKER, delta);
                                }
                            }

                            b.add(new EntityPositionSyncS2CPacket(id,
                                    new PlayerPosition(new Vec3d(pos.x + vec.x, pos.y + vec.y - 0.5, pos.z + vec.z), Vec3d.ZERO, 0, 0), false
                            ));
                        }
                    }
                }
            }
            if (this.getHolder() != null) {
                this.getHolder().sendPacket(new BundleS2CPacket(b));
            }
            this.quaternion.set(newQuaternion);
        }
        this.main.tick();
    }

    @Override
    public void setHolder(ElementHolder holder) {
        super.setHolder(holder);
        this.main.setHolder(holder);
    }

    public void setWorld(ServerWorld world) {
        if (this.world != null) {
            ((BlockCollectionView) this.world).polyfactory$removeCollision(this);
        }
        if (world != null) {
            ((BlockCollectionView) world).polyfactory$addCollision(this);
        }

        this.world = world;
    }

    @Override
    public void setInitialPosition(Vec3d newPos) {
        super.setInitialPosition(newPos);
        this.main.setInitialPosition(newPos);
    }

    @Override
    public void setOffset(Vec3d offset) {
        super.setOffset(offset);
        this.main.setOffset(offset);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return new WorldBorder();
    }

    @Nullable
    @Override
    public BlockView getChunkAsView(int chunkX, int chunkZ) {
        return this;
    }

    public void provideCollisions(Box box, Consumer<VoxelShape> consumer) {
        var pos = this.main.getCurrentPos();

        if (!box.intersects(
                pos.x - this.centerX - 2, pos.y - this.centerY - 2, pos.z - this.centerZ - 2,
                pos.x - this.centerX + this.sizeX + 2, pos.y - this.centerY + this.sizeY + 2, pos.z - this.centerZ + this.sizeZ + 2
        )) {
            return;
        }

        var vec = new Vector3f();

        for (var x = 0; x < this.sizeX; x++) {
            for (var y = 0; y < this.sizeY; y++) {
                for (var z = 0; z < this.sizeZ; z++) {
                    vec.set(x - this.centerX, y - this.centerY, z - this.centerZ).rotate(this.quaternion);
                    if (this.collisionBlockId2[index(x, y, z)] != -1 && box.intersects(
                            pos.x + vec.x - 0.5, pos.y + vec.y - 0.5, pos.z + vec.z - 0.5,
                            pos.x + vec.x + 0.5, pos.y + vec.y + 0.5, pos.z + vec.z + 0.5
                    )) {
                        consumer.accept(VoxelShapes.cuboid(
                                pos.x + vec.x - 0.5, pos.y + vec.y - 0.5, pos.z + vec.z - 0.5,
                                pos.x + vec.x + 0.5, pos.y + vec.y + 0.5, pos.z + vec.z + 0.5
                        ));
                    }
                }
            }
        }
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, Box box) {
        var arr = new ArrayList<VoxelShape>();
        this.provideCollisions(box, arr::add);
        return arr;
    }
}
