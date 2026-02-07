package eu.pb4.polyfactory.block.other;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ProxyAttachement implements BlockAwareAttachment, VirtualElement {
    private final Supplier<Vec3> posSupplier;
    private ServerLevel level;
    @Nullable
    private ElementHolder owner;
    private final ElementHolder holder;
    private final Supplier<BlockState> blockStateSupplier;

    public ProxyAttachement(ElementHolder holder,
                            Supplier<Vec3> pos,
                            Supplier<BlockState> blockStateSupplier,
                            ServerLevel level) {
        this.holder = holder;
        this.posSupplier = pos;
        this.blockStateSupplier = blockStateSupplier;
        this.level = level;
    }

    @Override
    public BlockPos getBlockPos() {
        return BlockPos.containing(this.posSupplier.get());
    }

    @Override
    public BlockState getBlockState() {
        return this.blockStateSupplier.get();
    }

    @Override
    public boolean isPartOfTheWorld() {
        return owner != null && owner.getAttachment() != null && owner.getAttachment().getWorld() != null;
    }

    @Override
    public ElementHolder holder() {
        return holder;
    }

    @Override
    public void destroy() {
        //this.holder.destroy();
    }

    @Override
    public Vec3 getPos() {
        return this.posSupplier.get();
    }

    @Override
    public ServerLevel getWorld() {
        return level;
    }

    @Override
    public void updateCurrentlyTracking(Collection<ServerGamePacketListenerImpl> collection) {
    }

    @Override
    public void updateTracking(ServerGamePacketListenerImpl serverPlayNetworkHandler) {
    }

    @Override
    public IntList getEntityIds() {
        return this.holder.getEntityIds();
    }

    @Override
    public void setHolder(@Nullable ElementHolder elementHolder) {
        this.owner = elementHolder;
    }

    @Override
    public @Nullable ElementHolder getHolder() {
        return this.owner;
    }

    @Override
    public Vec3 getOffset() {
        return Vec3.ZERO;
    }

    @Override
    public void setOffset(Vec3 vec3d) {
    }

    @Override
    public void startWatching(ServerPlayer serverPlayerEntity, Consumer<Packet<ClientGamePacketListener>> consumer) {
        this.holder.startWatching(serverPlayerEntity);
    }

    @Override
    public void stopWatching(ServerPlayer serverPlayerEntity, Consumer<Packet<ClientGamePacketListener>> consumer) {
        this.holder.stopWatching(serverPlayerEntity);
    }

    @Override
    public void notifyMove(Vec3 vec3d, Vec3 vec3d1, Vec3 vec3d2) {
    }

    @Override
    public InteractionHandler getInteractionHandler(ServerPlayer serverPlayerEntity) {
        return null;
    }

    @Override
    public void tick() {
        BlockAwareAttachment.super.tick();
        //this.holder.tick();
    }

    public ElementHolder owner() {
        return owner;
    }

    public Supplier<BlockState> blockStateSupplier() {
        return blockStateSupplier;
    }

    public void setLevel(ServerLevel level) {
        this.level = level;
    }
}
