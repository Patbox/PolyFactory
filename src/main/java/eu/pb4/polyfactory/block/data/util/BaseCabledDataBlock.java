package eu.pb4.polyfactory.block.data.util;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.block.data.AbstractCableBlock;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.block.data.ChannelContainer;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public abstract class BaseCabledDataBlock extends AbstractCableBlock implements FactoryBlock, ConfigurableBlock, EntityBlock, CableConnectable {
    public BaseCabledDataBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_CABLE);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return (context.getItemInHand().is(FactoryItems.CABLE) && !state.getValue(HAS_CABLE)) || super.canBeReplaced(state, context);
    }
    
    
    protected abstract Direction getFacing(BlockState state);

    @Override
    protected boolean isDirectionBlocked(BlockState state, Direction direction) {
        return getFacing(state) == direction || !state.getValue(HAS_CABLE);
    }

    @Override
    public boolean canCableConnect(LevelReader world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return getFacing(state) != dir && state.getValue(HAS_CABLE) && super.canCableConnect(world, cableColor, pos, state, dir);
    }

    @Override
    public boolean hasCable(BlockState state) {
        return state.getValue(HAS_CABLE);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(
                BlockConfig.CHANNEL
        );
    }

    @Override
    public abstract @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState);

    @Override
    public boolean setColor(BlockState state, Level world, BlockPos pos, int color) {
        return state.getValue(HAS_CABLE) && super.setColor(state, world, pos, color);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChanneledDataBlockEntity(pos, state);
    }

    protected int getChannel(ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ChannelContainer container) {
            return container.channel();
        }
        return 0;
    }

    public static abstract class Model extends BaseCableModel {
        protected final ItemDisplayElement base;

        protected Model(BlockState state) {
            super(state);
            this.base = createBaseModel(state);
            this.base.setScale(new Vector3f(2));

            updateStatePos(state);
            this.addElement(this.base);
        }

        protected ItemDisplayElement createBaseModel(BlockState state) {
            return ItemDisplayElementUtil.createSolid(state.getBlock().asItem());
        }

        protected abstract void updateStatePos(BlockState state);

        @Override
        protected void setState(BlockState blockState) {
            super.setState(blockState);
            updateStatePos(this.blockState());
            this.base.tick();
        }
    }
}
