package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public interface XInWallBlock extends TagRedirector, BlockWithElementHolder, PolymerBlock {
    Block backing();

    BlockState convertToBacking(BlockState state);

    @Override
    default boolean customIsIn(TagKey<Block> tagKey, boolean original) {
        return original || this.backing().builtInRegistryHolder().is(tagKey);
    }

    @Override
    default boolean customIsIn(HolderSet<Block> entryList, boolean original) {
        return original || entryList.contains(this.backing().builtInRegistryHolder());
    }

    @Override
    default @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        var converted = this.convertToBacking(initialBlockState);
        var holder = BlockWithElementHolder.get(converted);
        if (holder == null) {
            return null;
        }
        var elementHolder = holder.createElementHolder(world, pos, converted);
        if (elementHolder == null) {
            return null;
        }

        return new SimpleRedirectingModel(elementHolder, this::convertToBacking, world);
    }

    @Override
    default BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return this.convertToBacking(blockState);
    }

    class SimpleRedirectingModel extends BlockModel {
        private final ProxyAttachement proxied;
        public SimpleRedirectingModel(ElementHolder elementHolder, Function<BlockState, BlockState> convertToBacking, ServerLevel level) {
            this.proxied = new ProxyAttachement(elementHolder, () -> this.currentPos, () -> convertToBacking.apply(blockState()), level);
        }

        @Override
        public void setAttachment(@Nullable HolderAttachment attachment) {
            if (attachment == null) {
                this.removeElement(this.proxied);
                this.proxied.holder().setAttachment(null);
            } else {
                this.proxied.holder().setAttachment(this.proxied);
                this.addElement(proxied);
            }
            super.setAttachment(attachment);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            super.notifyUpdate(updateType);
            this.proxied.holder().notifyUpdate(updateType);
        }
    }

}
