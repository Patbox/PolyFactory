package eu.pb4.polyfactory.block.collection;

import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.other.ProxyAttachement;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.elements.AbstractElement;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import eu.pb4.polymer.virtualentity.api.tracker.EntityTrackedData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;

public class BlockCollection extends AbstractElement implements CollisionGetter {
    public static boolean ignoreCollisions = false;
    private final DisplayElement main = ItemDisplayElementUtil.createSimple(ItemStack.EMPTY, 2);
    private final int[] blockId;
    private final int[] collisionBlockId;
    private final int[] collisionBlockId2;
    private final ProxyAttachement[] proxyAttachements;
    private final IntList blockIdList = new IntArrayList();
    private final IntList allIdList = new IntArrayList();
    private final BlockCollectionData data;
    private final Quaternionf newQuaternion = Direction.UP.getRotation();
    private final Quaternionf quaternion = Direction.UP.getRotation();
    private float centerX;
    private float centerY;
    private float centerZ;
    @Nullable
    private ServerLevel level;
    private boolean quaternionDirty = false;
    private boolean disableCollision;
    private int ticksSinceFullResync = -1;

    private Int2ObjectMap<Vec3> previousShift = new Int2ObjectOpenHashMap<>();

    public BlockCollection(int sizeX, int sizeY, int sizeZ) {
        this(new BlockCollectionData(sizeX, sizeY, sizeZ));
    }

    public BlockCollection(BlockCollectionData data) {
        var size = data.size();
        this.blockId = new int[size];
        this.collisionBlockId = new int[size];
        this.collisionBlockId2 = new int[size];
        this.proxyAttachements = new ProxyAttachement[size];
        Arrays.fill(this.blockId, -1);
        Arrays.fill(this.collisionBlockId, -1);
        Arrays.fill(this.collisionBlockId2, -1);
        this.allIdList.add(this.main.getEntityId());
        this.data = data;
        for (int x = 0; x < data.sizeX(); x++) {
            for (int y = 0; y < data.sizeY(); y++) {
                for (int z = 0; z < data.sizeZ(); z++) {
                    this.updateBlockVisualsAt(x, y, z, this.data.getBlockState(x, y, z));
                }
            }
        }
    }

    public void setCenter(float x, float y, float z) {
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
    }

    @Override
    public void startWatching(ServerPlayer player, Consumer<Packet<ClientGamePacketListener>> packetConsumer) {
        this.main.startWatching(player, packetConsumer);
        var pos = this.main.getLastSyncedPos();
        var vec = new Vector3f();
        var quaternion = new Quaternionf(this.quaternion);
        for (var x = 0; x < this.data.sizeX(); x++) {
            for (var y = 0; y < this.data.sizeY(); y++) {
                for (var z = 0; z < this.data.sizeZ(); z++) {
                    sendInitialBlockVisual(packetConsumer, x, y, z, pos, vec, quaternion);
                    sendInitialBlockCollision(packetConsumer, x, y, z, pos, vec, quaternion);
                }
            }
        }

        packetConsumer.accept(VirtualEntityUtils.createRidePacket(this.main.getEntityId(), this.blockIdList));
    }

    public void sendInitialBlockVisual(Consumer<Packet<ClientGamePacketListener>> packetConsumer, int x, int y, int z, Vec3 pos, Vector3f vec, Quaternionf quaternion) {
        var i = index(x, y, z);
        var id = this.blockId[i];
        if (id != -1) {
            packetConsumer.accept(new ClientboundAddEntityPacket(id, UUID.randomUUID(),
                    pos.x, pos.y, pos.z, 0f, 0f,
                    EntityType.BLOCK_DISPLAY, 0, Vec3.ZERO, 0d
            ));

            packetConsumer.accept(new ClientboundSetEntityDataPacket(id, List.of(
                    SynchedEntityData.DataValue.create(DisplayTrackedData.INTERPOLATION_DURATION, 1),
                    SynchedEntityData.DataValue.create(DisplayTrackedData.Block.BLOCK_STATE, this.data.states()[i]),
                    SynchedEntityData.DataValue.create(DisplayTrackedData.TRANSLATION, new Vector3f(x - 0.5f - this.centerX, y - 0.5f - this.centerY, z - 0.5f - this.centerZ)
                            .rotate(quaternion)),
                    SynchedEntityData.DataValue.create(DisplayTrackedData.LEFT_ROTATION, quaternion),
                    SynchedEntityData.DataValue.create(EntityTrackedData.FLAGS, (byte) (1 << EntityTrackedData.INVISIBLE_FLAG_INDEX))

            )));
        }
    }

    public void sendInitialBlockCollision(Consumer<Packet<ClientGamePacketListener>> packetConsumer, int x, int y, int z, Vec3 pos, Vector3f vec, Quaternionf quaternion) {
        var i = index(x, y, z);
        var id = this.collisionBlockId[i];
        if (id != -1) {
            vec.set(x - this.centerX, y - this.centerY, z - this.centerZ).rotate(quaternion);
            packetConsumer.accept(new ClientboundAddEntityPacket(id, UUID.randomUUID(),
                    pos.x + vec.x, pos.y + vec.y - 0.5, pos.z + vec.z, 0f, 0f,
                    EntityType.SHULKER, 0, Vec3.ZERO, 0d
            ));

            packetConsumer.accept(new ClientboundSetEntityDataPacket(id, List.of(
                    SynchedEntityData.DataValue.create(EntityTrackedData.FLAGS, (byte) (1 << EntityTrackedData.INVISIBLE_FLAG_INDEX))
            )));

            packetConsumer.accept(new ClientboundAddEntityPacket(this.collisionBlockId2[i], UUID.randomUUID(),
                    pos.x + vec.x, pos.y + vec.y - 0.5, pos.z + vec.z, 0f, 0f,
                    EntityType.BLOCK_DISPLAY, 0, Vec3.ZERO, 0d
            ));

            packetConsumer.accept(new ClientboundSetEntityDataPacket(this.collisionBlockId2[i], List.of(
                    //DataTracker.SerializedEntry.of(DisplayTrackedData.INTERPOLATION_DURATION, 1),
                    SynchedEntityData.DataValue.create(EntityTrackedData.FLAGS, (byte) (1 << EntityTrackedData.INVISIBLE_FLAG_INDEX))
            )));

            packetConsumer.accept(VirtualEntityUtils.createRidePacket(this.collisionBlockId2[i], IntList.of(id)));
        }
    }

    public void setQuaternion(Quaternionf quaternion) {
        if (!this.newQuaternion.equals(quaternion)) {
            this.newQuaternion.set(quaternion);
            this.quaternionDirty = true;
        }
    }

    @Override
    public Vec3 getLastSyncedPos() {
        if (this.lastSyncedPos == null) {
            this.updateLastSyncedPos();
        }

        return this.lastSyncedPos;
    }

    public void setBlockState(int x, int y, int z, BlockState state, @Nullable BlockEntity blockEntity) {
        if (!this.data.setBlockState(x, y, z, state, blockEntity)) {
            return;
        }
        this.updateBlockVisualsAt(x, y, z, state);
    }

    public void updateBlockVisualsAt(int x, int y, int z, BlockState state) {
        this.tryUpdatingAttachment(x, y, z, state);

        var i = index(x, y, z);
        if (this.blockId[i] == -1 && !state.isAir()) {
            var e = VirtualEntityUtils.requestEntityId();
            this.blockId[i] = e;
            this.blockIdList.add(e);
            this.allIdList.add(e);

            var pos = this.main.getLastSyncedPos();
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
            if (this.blockId[i] != -1) {
                this.blockIdList.removeInt(this.blockId[i]);
                this.allIdList.removeInt(this.blockId[i]);
            }
            if (this.collisionBlockId[i] != -1) {
                this.allIdList.removeInt(this.collisionBlockId[i]);
                this.allIdList.removeInt(this.collisionBlockId2[i]);
                if (this.getHolder() != null) {
                    this.getHolder().sendPacket(new ClientboundRemoveEntitiesPacket(IntList.of(
                            this.blockId[i], this.collisionBlockId[i], this.collisionBlockId2[i])));
                }
            } else if (this.getHolder() != null && this.blockId[i] != -1) {
                this.getHolder().sendPacket(new ClientboundRemoveEntitiesPacket(IntList.of(this.blockId[i])));
            }
            this.blockId[i] = this.collisionBlockId[i] = this.collisionBlockId2[i] = -1;
        } else {
            var noCollision = state.getCollisionShape(this, new BlockPos(x, y, z)).isEmpty();
            if (this.getHolder() != null) {
                this.getHolder().sendPacket(new ClientboundSetEntityDataPacket(this.blockId[i], List.of(
                        SynchedEntityData.DataValue.create(DisplayTrackedData.Block.BLOCK_STATE, this.data.states()[i])
                )));
            }

            if (this.collisionBlockId[i] != -1 && noCollision) {
                this.allIdList.removeInt(this.collisionBlockId[i]);
                this.allIdList.removeInt(this.collisionBlockId2[i]);
                if (this.getHolder() != null) {
                    this.getHolder().sendPacket(new ClientboundRemoveEntitiesPacket(IntList.of(this.collisionBlockId[i], this.collisionBlockId2[i])));
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
                    var pos = this.main.getLastSyncedPos();
                    var vec = new Vector3f();
                    var quaternion = new Quaternionf(this.quaternion);
                    this.sendInitialBlockCollision(this.getHolder()::sendPacket, x, y, z, pos, vec, quaternion);
                }
            }
        }
    }

    private void tryUpdatingAttachment(int x, int y, int z, BlockState state) {
        if (this.level == null) {
            return;
        }
        var i = index(x, y, z);

        if (BlockWithElementHolder.get(state) instanceof BlockWithElementHolder blockWithElementHolder) {
            if (this.proxyAttachements[i] == null || this.proxyAttachements[i].blockStateSupplier().get().getBlock() != state.getBlock()) {
                if (this.proxyAttachements[i] != null) {
                    if (this.getHolder() != null) {
                        this.getHolder().removeElement(this.proxyAttachements[i]);
                    }
                    this.proxyAttachements[i].holder().destroy();
                }
                var pos = new BlockPos.MutableBlockPos(x, y, z);

                var elementHolder = blockWithElementHolder.createElementHolder(this.level, pos, state);
                if (elementHolder != null) {
                    this.proxyAttachements[i] = new ProxyAttachement(elementHolder, () -> {
                        var offset = new Vector3f(x - this.centerX, y - this.centerY, z - this.centerZ)
                                .rotate(quaternion);
                        return this.getCurrentPos().add(offset.x, offset.y, offset.z);
                    }, () -> this.data.states()[i], this.level);

                    elementHolder.setAttachment(this.proxyAttachements[i]);
                    if (this.getHolder() != null) {
                        this.getHolder().addElement(this.proxyAttachements[i]);
                    }
                } else {
                    this.proxyAttachements[i] = null;
                }
            } else if (this.proxyAttachements[i] != null) {
                if (this.getHolder() != null) {
                    this.getHolder().removeElement(this.proxyAttachements[i]);
                }
                this.proxyAttachements[i] = null;
            }
        } else if (this.proxyAttachements[i] != null) {
            if (this.getHolder() != null) {
                this.getHolder().removeElement(this.proxyAttachements[i]);
            }
            this.proxyAttachements[i] = null;
        }
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        var i = index(pos.getX(), pos.getY(), pos.getZ());
        return i != -1 ? this.data.blockEntities()[i] : null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockState getBlockState(int x, int y, int z) {
        var i = index(x, y, z);
        return i != -1 ? this.data.states()[i] : Blocks.VOID_AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    private int index(int x, int y, int z) {
        return this.data.index(x, y, z);
    }

    @Override
    public int getHeight() {
        return this.data.sizeY();
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public IntList getEntityIds() {
        return this.allIdList;
    }

    @Override
    public void stopWatching(ServerPlayer player, Consumer<Packet<ClientGamePacketListener>> packetConsumer) {
        this.main.stopWatching(player, packetConsumer);
    }

    @Override
    public void notifyMove(Vec3 oldPos, Vec3 currentPos, Vec3 delta) {
        this.main.notifyMove(oldPos, currentPos, delta);
    }

    @Override
    public void tick() {
        var newPreviousShift = new Int2ObjectOpenHashMap<Vec3>();
        var actuallyDirty = this.quaternionDirty || !this.getCurrentPos().equals(this.getLastSyncedPos());
        if (actuallyDirty || (this.ticksSinceFullResync != -1 && this.ticksSinceFullResync++ % 300 == 0)) {
            var refreshPos = this.ticksSinceFullResync % 300 == 1;
            if (this.ticksSinceFullResync > 300 && !actuallyDirty) {
                this.ticksSinceFullResync = -1;
            }

            var pos = this.getCurrentPos();
            var previous = this.getLastSyncedPos();
            if (previous == null) {
                previous = pos;
            }
            var vec = new Vector3f();
            var vec2 = new Vector3f();
            var b = new ArrayList<Packet<? super ClientGamePacketListener>>();
            var box = new AABB(pos.x - this.centerX - 2, pos.y - this.centerY - 2, pos.z - this.centerZ - 2,
                    pos.x - this.centerX + this.data.sizeX() + 2, pos.y - this.centerY + this.data.sizeY() + 2, pos.z - this.centerZ + this.data.sizeZ() + 2);

            var ents = this.level != null ? this.level.getEntities((Entity) null, box, EntitySelector.NO_SPECTATORS) : List.<Entity>of();
            var quaternion = new Quaternionf(this.newQuaternion);
            var quaternionOldInverted = new Quaternionf(this.quaternion).invert();
            var diff = pos.subtract(previous);

            var idMap = new IdentityHashMap<Entity, Vector3f>();

            for (var x = 0; x < this.data.sizeX(); x++) {
                for (var y = 0; y < this.data.sizeY(); y++) {
                    for (var z = 0; z < this.data.sizeZ(); z++) {
                        var i = index(x, y, z);
                        var id = this.blockId[i];
                        if (id != -1) {
                            b.add(new ClientboundSetEntityDataPacket(id, List.of(
                                    SynchedEntityData.DataValue.create(DisplayTrackedData.TRANSLATION, new Vector3f(x - 0.5f - this.centerX, y - 0.5f - this.centerY, z - 0.5f - this.centerZ).rotate(quaternion)),
                                    SynchedEntityData.DataValue.create(DisplayTrackedData.LEFT_ROTATION, quaternion),
                                    SynchedEntityData.DataValue.create(DisplayTrackedData.START_INTERPOLATION, 0)
                            )));
                        }
                        id = this.collisionBlockId2[i];
                        if (id != -1) {
                            vec.set(x - this.centerX, y - this.centerY, z - this.centerZ);
                            vec.rotate(this.newQuaternion);
                            b.add(new ClientboundEntityPositionSyncPacket(id,
                                    new PositionMoveRotation(new Vec3(pos.x + vec.x, pos.y + vec.y - 0.5, pos.z + vec.z), Vec3.ZERO, 0, 0), false
                            ));
                            vec2.set(x - this.centerX, y - this.centerY, z - this.centerZ);
                            vec2.rotate(this.quaternion);

                            for (var entity : ents) {
                                if (entity.getBoundingBox().intersects(
                                        previous.x + vec2.x - 0.5 - 0.05, previous.y + vec2.y - 0.5 - 0.05, previous.z + vec2.z - 0.5 - 0.05,
                                        previous.x + vec2.x + 0.5 + 0.05, previous.y + vec2.y + 0.5 + 0.25, previous.z + vec2.z + 0.5 + 0.05
                                )) {
                                    var evec = idMap.computeIfAbsent(entity, (a) -> new Vector3f());
                                    vec.sub(vec2);
                                    evec.y = Math.max(evec.y, vec.y);

                                    vec.set(entity.getX() - previous.x(), entity.getY() - previous.y(), entity.getZ() - previous.z());
                                    vec.rotate(quaternionOldInverted);
                                    vec.rotate(quaternion);

                                    evec.x = (float) (vec.x - entity.getX() + previous.x());
                                    evec.z = (float) (vec.z - entity.getZ() + previous.z());
                                }
                            }
                        }
                    }
                }
            }
            this.disableCollision = true;

            if (actuallyDirty) {
                idMap.forEach((entity, val) -> {
                    var move = diff.add(val.x, val.y, val.z);
                    if (!Mth.equal(move.lengthSqr(), 0)) {
                        entity.move(MoverType.SHULKER, move);
                        if (entity instanceof ServerPlayer player) {
                            //FactoryUtil.sendVelocityDelta(player, move);
                            FactoryUtil.runNextTick(() -> player.connection.teleport(new PositionMoveRotation(move, Vec3.ZERO, 0, 0),
                                    EnumSet.of(Relative.X, Relative.Y, Relative.Z, Relative.DELTA_X, Relative.DELTA_Y, Relative.DELTA_Z, Relative.Y_ROT, Relative.X_ROT
                                    )));

                            //newPreviousShift.put(player.getId(), move);
                        }
                    }
                });
            }

            this.disableCollision = false;
            if (this.getHolder() != null) {
                this.getHolder().sendPacket(new ClientboundBundlePacket(b));
            }
            this.quaternion.set(newQuaternion);
            this.updateLastSyncedPos();
        }
        if (this.level != null) {
            this.previousShift.forEach((entity, val) -> {
                if (level.getEntity(entity) instanceof ServerPlayer player) {
                    var oldMove = this.previousShift.getOrDefault(player.getId(), Vec3.ZERO);
                    //FactoryUtil.sendVelocityDelta(player, oldMove.multiply(0.54).negate());
                }
            });
        }
        this.previousShift = newPreviousShift;
        this.main.tick();
    }

    @Override
    public void setHolder(ElementHolder holder) {
        if (this.getHolder() == holder) {
            return;
        } else if (this.getHolder() != null) {
            for (var proxy : this.proxyAttachements) {
                if (proxy != null) {
                    this.getHolder().removeElement(proxy);
                }
            }
        }

        super.setHolder(holder);
        this.main.setHolder(holder);

        if (holder != null) {
            for (var proxy : this.proxyAttachements) {
                if (proxy != null) {
                    holder.addElement(proxy);
                }
            }
        }
    }

    public void setLevel(ServerLevel level) {
        if (this.level == level) {
            return;
        }

        if (this.level != null) {
            ((BlockCollectionView) this.level).polyfactory$removeCollision(this);

            for (var proxy : this.proxyAttachements) {
                if (proxy != null) {
                    proxy.setLevel(null);
                    proxy.holder().destroy();
                }
            }
            Arrays.fill(this.proxyAttachements, null);
        }
        this.level = level;

        if (level != null) {
            ((BlockCollectionView) level).polyfactory$addCollision(this);
            for (var x = 0; x < this.data.sizeX(); x++) {
                for (var y = 0; y < this.data.sizeY(); y++) {
                    for (var z = 0; z < this.data.sizeZ(); z++) {
                        this.tryUpdatingAttachment(x, y, z, this.data.getBlockState(x, y, z));
                    }
                }
            }
        }
    }

    @Override
    public void setInitialPosition(Vec3 newPos) {
        super.setInitialPosition(newPos);
        this.main.setInitialPosition(newPos);
    }

    @Override
    public void setOffset(Vec3 offset) {
        super.setOffset(offset);
        this.main.setOffset(offset);
    }

    @Nullable
    @Override
    public void setOverridePos(Vec3 vec3d) {
        super.setOverridePos(vec3d);
        this.main.setOverridePos(vec3d);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return new WorldBorder();
    }

    @Nullable
    @Override
    public BlockGetter getChunkForCollisions(int chunkX, int chunkZ) {
        return this;
    }

    public void provideCollisions(AABB box, Consumer<VoxelShape> consumer) {
        if (this.disableCollision || ignoreCollisions) {
            return;
        }

        var pos = this.main.getLastSyncedPos();

        if (!box.intersects(
                pos.x - this.centerX - 2, pos.y - this.centerY - 2, pos.z - this.centerZ - 2,
                pos.x - this.centerX + this.data.sizeX() + 2, pos.y - this.centerY + this.data.sizeY() + 2, pos.z - this.centerZ + this.data.sizeZ() + 2
        )) {
            return;
        }

        var vec = new Vector3f();

        for (var x = 0; x < this.data.sizeX(); x++) {
            for (var y = 0; y < this.data.sizeY(); y++) {
                for (var z = 0; z < this.data.sizeZ(); z++) {
                    vec.set(x - this.centerX, y - this.centerY, z - this.centerZ).rotate(this.quaternion);
                    if (this.collisionBlockId2[index(x, y, z)] != -1 && box.intersects(
                            pos.x + vec.x - 0.5, pos.y + vec.y - 0.5, pos.z + vec.z - 0.5,
                            pos.x + vec.x + 0.5, pos.y + vec.y + 0.5, pos.z + vec.z + 0.5
                    )) {
                        consumer.accept(Shapes.box(
                                pos.x + vec.x - 0.5, pos.y + vec.y - 0.5, pos.z + vec.z - 0.5,
                                pos.x + vec.x + 0.5, pos.y + vec.y + 0.5, pos.z + vec.z + 0.5
                        ));
                    }
                }
            }
        }
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB box) {
        var arr = new ArrayList<VoxelShape>();
        this.provideCollisions(box, arr::add);
        return arr;
    }
}
