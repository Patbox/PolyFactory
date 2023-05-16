package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.display.LodItemDisplayElement;
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

public class MovingItemContainer implements VirtualElement, StackReference {
    private ItemStack stack;
    private ItemStack stackCurrent;
    private final ItemDisplayElement itemDisplay[] = new ItemDisplayElement[4];
    private final GenericEntityElement riddenBase = new MarkerElement() {
        @Override
        protected Packet<ClientPlayPacketListener> createSpawnPacket(ServerPlayerEntity player) {
            return new EntitySpawnS2CPacket(this.getEntityId(), this.getUuid(), pos.x, pos.y, pos.z, 0, 0, this.getEntityType(), 0, Vec3d.ZERO, 0);
        }
    };
    private Vec3d pos;

    public MovingItemContainer(ItemStack stack) {
        this.stack = stack.copy();
        this.stackCurrent = stack.copy();

        for (var i = 0; i < 4; i++) {
            this.itemDisplay[i] = new LodItemDisplayElement();
            this.itemDisplay[i].setLeftRotation(Direction.NORTH.getRotationQuaternion());
            this.itemDisplay[i].setDisplayWidth(1);
            this.itemDisplay[i].setDisplayHeight(2);
            this.itemDisplay[i].setViewRange(0.8f);
            this.itemDisplay[i].setModelTransformation(ModelTransformationMode.FIXED);
            this.itemDisplay[i].setInterpolationDuration(10);
            this.updateDisplay(i);
        }
    }

    public MovingItemContainer split(Aware aware) {
        return split(aware.getMaxStackCount(this.stack));
    }

    public MovingItemContainer split(int newCount) {
        var x = new MovingItemContainer(this.stack.copyWithCount(newCount));
        this.stack.decrement(newCount);
        return x;
    }

    private void updateDisplay(int i) {
        var count = this.stack.getCount();

        if (i * 16 <= count) {
            this.itemDisplay[i].setItem(this.stack);
        } else {
            this.itemDisplay[i].setItem(ItemStack.EMPTY);
        }

        this.itemDisplay[i].setScale(new Vector3f(stack.getItem() instanceof BlockItem ? 0.9f : 0.5f));
    }

    @Override
    public ItemStack get() {
        return this.stack;
    }

    @Override
    public boolean set(ItemStack stack) {
        if (ItemStack.canCombine(this.stack, stack)) {
            return false;
        }
        this.stack = stack;
        for (var i = 0; i < 4; i++) {
            updateDisplay(i);
        }
        return true;
    }

    public void setPos(Vec3d vec3d) {
        if (this.getHolder() != null) {
            //this.getHolder().sendPacket(VirtualEntityUtils.createSimpleMovePacket(this.riddenBase.getEntityId(), vec3d, (byte) 0, (byte) 0));
            this.riddenBase.notifyMove(this.pos, vec3d, vec3d.subtract(this.pos));
        }
        this.pos = vec3d;
    }

    @Override
    public IntList getEntityIds() {
        return IntList.of(this.riddenBase.getEntityId(), this.itemDisplay[0].getEntityId(), this.itemDisplay[1].getEntityId(), this.itemDisplay[2].getEntityId(), this.itemDisplay[3].getEntityId());
    }

    @Override
    public void setHolder(@Nullable ElementHolder holder) {
        this.riddenBase.setHolder(holder);
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
        this.riddenBase.startWatching(player, packetConsumer);
        for (var x : itemDisplay) {
            x.startWatching(player, packetConsumer);
        }
        packetConsumer.accept(VirtualEntityUtils.createRidePacket(this.riddenBase.getEntityId(), IntList.of(this.itemDisplay[0].getEntityId(), this.itemDisplay[1].getEntityId(), this.itemDisplay[2].getEntityId(), this.itemDisplay[3].getEntityId())));
    }

    @Override
    public void stopWatching(ServerPlayerEntity player, Consumer<Packet<ClientPlayPacketListener>> packetConsumer) {}

    @Override
    public void notifyMove(Vec3d oldPos, Vec3d currentPos, Vec3d delta) {
    }

    @Override
    public void tick() {
        for (var x : this.itemDisplay) {
            if (x.isDirty()) {
                x.tick();
            }
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
        for (var i = 0; i < 4; i++) {
            var display = this.itemDisplay[i];
            var tr = quaternionf.mul(Direction.NORTH.getRotationQuaternion(), new Quaternionf());
            display.setLeftRotation(tr);
            var e = i % 2 == 1;
            display.setTranslation(new Vector3f( (e ? i : -i) * 0.01f, i * 0.03f, (e ? i : -i) * 0.01f).rotate(quaternionf));
            if (display.isDirty()) {
                display.startInterpolation();
            }
        }
    }

    public void checkItems() {
        if (this.stack.getCount() != this.stackCurrent.getCount() || ItemStack.canCombine(this.stack, this.stackCurrent)) {
            this.stackCurrent = this.stack.copy();
            for (var i = 0; i < 4; i++) {
                this.updateDisplay(i);
            }
        }
    }

    public interface Aware {
        @Nullable
        MovingItemContainer getContainer();
        void setContainer(@Nullable MovingItemContainer container);
        default void clearContainer() {
            setContainer(null);
        }
        @Nullable
        MovingItemContainer pullAndRemove();

        void pushAndAttach(MovingItemContainer container);

        default boolean isContainerEmpty() {
            return getContainer() == null;
        }

        default double movementDelta() {
            return 1;
        }

        default void setMovementPosition(double pos) {
        }

        default int getMaxStackCount(ItemStack stack) {
            return stack.getMaxCount();
        }

        default boolean pushNew(ItemStack stack) {
            if (!isContainerEmpty()) {
                return false;
            }

            var i = Math.min(stack.getCount(), getMaxStackCount(stack));
            var moving = new MovingItemContainer(stack.copyWithCount(i));
            stack.decrement(i);

            setContainer(moving);
            return true;
        }
    }

    public interface AwareRedirecting extends Aware {
        void setContainerLocal(MovingItemContainer container);

        @Nullable
        Aware getRedirect();

        @Override
        default void setContainer(MovingItemContainer container) {
            this.setContainerLocal(container);
            var x= getRedirect();
            if (x != null) {
                x.setContainer(container);
            }
        }

        @Override
        default MovingItemContainer pullAndRemove() {
            var redirect = getRedirect();
            if (redirect != null) {
                this.setContainerLocal(null);
                return redirect.pullAndRemove();
            }
            var x = this.getContainer();
            this.setContainerLocal(null);
            return x;
        }

        @Override
        default void pushAndAttach(MovingItemContainer container) {
            this.setContainerLocal(container);

            var redirect = getRedirect();
            if (redirect != null) {
                redirect.pushAndAttach(container);
            }
        }
    }
}
