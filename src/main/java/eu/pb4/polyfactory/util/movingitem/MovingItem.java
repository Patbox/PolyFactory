package eu.pb4.polyfactory.util.movingitem;

import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.elements.*;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class MovingItem implements VirtualElement, StackReference {
    private ItemStack stack;
    private ItemStack stackCurrent;
    private final ItemDisplayElement[] itemDisplay = new ItemDisplayElement[4];
    private final ItemDisplayElement riddenBase;
    private float globalScale = 1;
    private int tick;
    private boolean isFirstMove = true;
    private Vec3d pos = Vec3d.ZERO;

    @Deprecated
    public MovingItem(ItemStack stack) {
        this(stack, Vec3d.ZERO);
    }

    public MovingItem(ItemStack stack, Vec3d pos) {
        this.stack = stack.copy();
        this.stackCurrent = stack.copy();

        for (var i = 0; i < 4; i++) {
            this.itemDisplay[i] = new LodItemDisplayElement();
            this.itemDisplay[i].setLeftRotation(Direction.NORTH.getRotationQuaternion());
            this.itemDisplay[i].setDisplayWidth(1);
            this.itemDisplay[i].setDisplayHeight(2);
            this.itemDisplay[i].setViewRange(i == 0 ? 0.5f : 0.2f);
            this.itemDisplay[i].setModelTransformation(ModelTransformationMode.FIXED);
            this.itemDisplay[i].setInterpolationDuration(10);
            this.itemDisplay[i].ignorePositionUpdates();
            this.updateDisplay(i);
        }
        this.riddenBase = this.itemDisplay[0];
        this.riddenBase.setSendPositionUpdates(true);
        this.setPos(pos);
    }

    public MovingItem split(ContainerHolder aware) {
        return split(aware.getMaxStackCount(this.stack));
    }

    public MovingItem split(int newCount) {
        var x = new MovingItem(this.stack.copyWithCount(newCount), this.getCurrentPos());
        this.stack.decrement(newCount);
        return x;
    }

    private void updateDisplay(int i) {
        var count = this.stack.getCount();

        if (i * 16 <= count) {
            this.itemDisplay[i].setItem(this.stack.copy());
        } else {
            this.itemDisplay[i].setItem(ItemStack.EMPTY);
        }

        this.updateScale(i);
    }

    private void updateScale(int i) {
        this.itemDisplay[i].setScale(new Vector3f((stack.getItem() instanceof BlockItem ? 0.9f : 0.5f) * this.globalScale ));
    }

    @Override
    public ItemStack get() {
        return this.stack;
    }

    @Override
    public boolean set(ItemStack stack) {
        if (ItemStack.areItemsAndComponentsEqual(this.stack, stack)) {
            return false;
        }
        this.stack = stack;
        for (var i = 0; i < 4; i++) {
            updateDisplay(i);
        }
        return true;
    }

    public void setPos(Vec3d vec3d) {
        this.setOverridePos(vec3d);
        for (var x : this.itemDisplay) {
            x.setOverridePos(vec3d);
        }
    }

    @Nullable
    @Override
    public void setOverridePos(Vec3d vec3d) {
        this.pos = vec3d;
    }

    @Override
    public @Nullable Vec3d getOverridePos() {
        return this.pos;
    }

    @Override
    public IntList getEntityIds() {
        return IntList.of(this.riddenBase.getEntityId(), this.itemDisplay[1].getEntityId(), this.itemDisplay[2].getEntityId(), this.itemDisplay[3].getEntityId());
    }

    @Override
    public void setHolder(@Nullable ElementHolder holder) {
        for (var x : itemDisplay) {
            x.setHolder(holder);
        }
    }

    @Override
    public @Nullable ElementHolder getHolder() {
        return this.riddenBase.getHolder();
    }

    @Override
    public Vec3d getOffset() {
        return Vec3d.ZERO;
    }

    @Override
    public void setOffset(Vec3d vec3d) {
    }

    @Override
    public void startWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {
        for (var x : itemDisplay) {
            x.startWatching(player, packetConsumer);
        }
        packetConsumer.accept(VirtualEntityUtils.createRidePacket(this.riddenBase.getEntityId(), IntList.of(this.itemDisplay[1].getEntityId(), this.itemDisplay[2].getEntityId(), this.itemDisplay[3].getEntityId())));
    }

    @Override
    public void stopWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {}

    @Override
    public void notifyMove(Vec3d oldPos, Vec3d currentPos, Vec3d delta) {
    }

    @Override
    public void tick() {
        if (this.tick++ % 2 == 1) {
            return;
        }
        this.checkItems();

        for (var x : this.itemDisplay) {
            x.tick();
        }

        if (this.isFirstMove) {
            this.riddenBase.setTeleportDuration(4);
            this.isFirstMove = false;
        }

    }

    @Override
    public InteractionHandler getInteractionHandler(ServerPlayerEntity player) {
        return InteractionHandler.EMPTY;
    }

    public void removeHolder(ElementHolder animation) {
        if (this.riddenBase.getHolder() == animation) {
            this.setHolder(null);
        }
    }

    public void setRotation(Quaternionf quaternionf) {
        if (quaternionf.equals(this.itemDisplay[0].getLeftRotation(), 0.05f)) {
            return;
        }

        for (var i = 0; i < 4; i++) {
            var display = this.itemDisplay[i];
            display.setLeftRotation(quaternionf);
            var e = i % 2 == 1;
            display.setTranslation(new Vector3f( (e ? i : -i) * 0.01f, i * 0.03f, (e ? i : -i) * 0.01f).rotate(quaternionf));
            if (display.isDirty()) {
                display.startInterpolation();
            }
        }
    }

    public void checkItems() {
        if (this.stack.getCount() != this.stackCurrent.getCount() || ItemStack.areItemsAndComponentsEqual(this.stack, this.stackCurrent)) {
            this.stackCurrent = this.stack.copy();
            for (var i = 0; i < 4; i++) {
                this.updateDisplay(i);
            }
        }
    }

    public void scale(float v) {
        if (this.globalScale == v) {
            return;
        }
        this.globalScale = v;
        for (int i = 0; i < 4; i++) {
            this.updateScale(i);
        }
    }
}
