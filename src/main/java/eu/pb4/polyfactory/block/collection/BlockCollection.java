package eu.pb4.polyfactory.block.collection;

import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.elements.AbstractElement;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class BlockCollection extends AbstractElement implements BlockView {
    private final DisplayElement main = ItemDisplayElementUtil.createSimple(ItemStack.EMPTY, 4);
    private final int[] blockId;
    private final IntList blockIdList = new IntArrayList();
    private final IntList allIdList = new IntArrayList();
    private final BlockState[] states;
    private final BlockEntity[] blockEntities;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    private Quaternionf quaternion = Direction.UP.getRotationQuaternion();
    private boolean quaternionDirty = false;

    public BlockCollection(int x, int y, int z) {
        var size = x * y * z;
        this.states = new BlockState[size];
        this.blockEntities = new BlockEntity[size];
        this.blockId = new int[size];
        Arrays.fill(this.blockId, -1);
        this.sizeX = x;
        this.sizeY = y;
        this.sizeZ = z;
        this.allIdList.add(this.main.getEntityId());
    }

    @Override
    public void startWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
        this.main.startWatching(player, packetConsumer);
        var pos = this.getHolder().getPos();
        for (var x = 0; x < this.sizeX; x++) {
            for (var y = 0; y < this.sizeY; y++) {
                for (var z = 0; z < this.sizeZ; z++) {
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
                                DataTracker.SerializedEntry.of(DisplayTrackedData.TRANSLATION, new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f)
                                        .rotate(this.quaternion)),
                                DataTracker.SerializedEntry.of(DisplayTrackedData.LEFT_ROTATION, this.quaternion)
                        )));
                    }
                }
            }
        }

        packetConsumer.accept(VirtualEntityUtils.createRidePacket(this.main.getEntityId(), this.blockIdList));
    }

    public void setQuaternion(Quaternionf quaternion) {
        this.quaternion = quaternion;
        this.quaternionDirty = true;
    }

    public void setBlockState(int x, int y, int z, BlockState state, @Nullable BlockEntity blockEntity) {
        var i = index(x, y, z);
        this.states[i] = state;
        this.blockEntities[i] = blockEntity;
        if (this.blockId[i] == -1 && !state.isAir()) {
            var e = VirtualEntityUtils.requestEntityId();
            this.blockId[i] = e;
            this.blockIdList.add(e);
            this.allIdList.add(e);
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
        return 0;
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
            var b = new ArrayList<Packet<? super ClientPlayPacketListener>>();
            for (var x = 0; x < this.sizeX; x++) {
                for (var y = 0; y < this.sizeY; y++) {
                    for (var z = 0; z < this.sizeZ; z++) {
                        var i = index(x, y, z);
                        var id = this.blockId[i];
                        if (id != -1) {
                            b.add(new EntityTrackerUpdateS2CPacket(id, List.of(
                                    DataTracker.SerializedEntry.of(DisplayTrackedData.TRANSLATION, new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f).rotate(this.quaternion)),
                                    DataTracker.SerializedEntry.of(DisplayTrackedData.LEFT_ROTATION, this.quaternion),
                                    DataTracker.SerializedEntry.of(DisplayTrackedData.START_INTERPOLATION, 0)
                            )));
                        }
                    }
                }
            }
            if (this.getHolder() != null) {
                this.getHolder().sendPacket(new BundleS2CPacket(b));
            }
        }
        this.main.tick();
    }

    @Override
    public void setHolder(ElementHolder holder) {
        super.setHolder(holder);
        this.main.setHolder(holder);
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
}
