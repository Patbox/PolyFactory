package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface XInWallBlock extends TagRedirector, BlockWithElementHolder, PolymerBlock {
    Block backing();

    BlockState convertToBacking(BlockState state);

    @Override
    default boolean customIsIn(TagKey<Block> tagKey, boolean original) {
        return original || this.backing().getRegistryEntry().isIn(tagKey);
    }

    @Override
    default boolean customIsIn(RegistryEntryList<Block> entryList, boolean original) {
        return original || entryList.contains(this.backing().getRegistryEntry());
    }

    @Override
    default @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        var converted = this.convertToBacking(initialBlockState);
        var holder = BlockWithElementHolder.get(converted);
        if (holder == null) {
            return null;
        }
        var elementHolder = holder.createElementHolder(world, pos, converted);
        if (elementHolder == null) {
            return null;
        }

        return new SimpleRedirectingModel(elementHolder, this::convertToBacking);
    }

    @Override
    default BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return this.convertToBacking(blockState);
    }

    class SimpleRedirectingModel extends BlockModel {
        private final ProxyAttachement proxied;
        public SimpleRedirectingModel(ElementHolder elementHolder, Function<BlockState, BlockState> convertToBacking) {
            this.proxied = new ProxyAttachement(this, elementHolder, () -> convertToBacking.apply(blockState()));
            elementHolder.setAttachment(this.proxied);
            this.addElement(proxied);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            super.notifyUpdate(updateType);
            this.proxied.holder.notifyUpdate(updateType);
        }
    }

    record ProxyAttachement(ElementHolder owner, ElementHolder holder, Supplier<BlockState> blockStateSupplier) implements BlockAwareAttachment, VirtualElement {
        @Override
        public BlockPos getBlockPos() {
            return BlockPos.ofFloored(owner.getPos());
        }

        @Override
        public BlockState getBlockState() {
            return this.blockStateSupplier.get();
        }

        @Override
        public boolean isPartOfTheWorld() {
            return owner.getAttachment() != null && owner.getAttachment().getWorld() != null;
        }

        @Override
        public ElementHolder holder() {
            return holder;
        }

        @Override
        public void destroy() {
            this.holder.destroy();
        }

        @Override
        public Vec3d getPos() {
            return owner.getPos();
        }

        @Override
        public ServerWorld getWorld() {
            return owner.getAttachment().getWorld();
        }

        @Override
        public void updateCurrentlyTracking(Collection<ServerPlayNetworkHandler> collection) {}

        @Override
        public void updateTracking(ServerPlayNetworkHandler serverPlayNetworkHandler) {}

        @Override
        public IntList getEntityIds() {
            return this.holder.getEntityIds();
        }

        @Override
        public void setHolder(@Nullable ElementHolder elementHolder) {}

        @Override
        public @Nullable ElementHolder getHolder() {
            return this.owner;
        }

        @Override
        public Vec3d getOffset() {
            return Vec3d.ZERO;
        }

        @Override
        public void setOffset(Vec3d vec3d) {}

        @Override
        public void startWatching(ServerPlayerEntity serverPlayerEntity, Consumer<Packet<ClientPlayPacketListener>> consumer) {
            this.holder.startWatching(serverPlayerEntity);
        }

        @Override
        public void stopWatching(ServerPlayerEntity serverPlayerEntity, Consumer<Packet<ClientPlayPacketListener>> consumer) {
            this.holder.stopWatching(serverPlayerEntity);
        }

        @Override
        public void notifyMove(Vec3d vec3d, Vec3d vec3d1, Vec3d vec3d2) {}

        @Override
        public InteractionHandler getInteractionHandler(ServerPlayerEntity serverPlayerEntity) {
            return null;
        }

        @Override
        public void tick() {
            BlockAwareAttachment.super.tick();
            this.holder.tick();
        }
    }
}
