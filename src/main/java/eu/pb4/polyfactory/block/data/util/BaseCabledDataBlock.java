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
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public abstract class BaseCabledDataBlock extends AbstractCableBlock implements FactoryBlock, ConfigurableBlock, BlockEntityProvider, CableConnectable {
    public BaseCabledDataBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_CABLE);
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        return (context.getStack().isOf(FactoryItems.CABLE) && !state.get(HAS_CABLE)) || super.canReplace(state, context);
    }
    
    
    protected abstract Direction getFacing(BlockState state);

    @Override
    protected boolean isDirectionBlocked(BlockState state, Direction direction) {
        return getFacing(state) == direction || !state.get(HAS_CABLE);
    }

    @Override
    public boolean canCableConnect(WorldView world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return getFacing(state) != dir && state.get(HAS_CABLE) && super.canCableConnect(world, cableColor, pos, state, dir);
    }

    @Override
    public boolean hasCable(BlockState state) {
        return state.get(HAS_CABLE);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(
                BlockConfig.CHANNEL
        );
    }

    @Override
    public abstract @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState);

    @Override
    public boolean setColor(BlockState state, World world, BlockPos pos, int color) {
        return state.get(HAS_CABLE) && super.setColor(state, world, pos, color);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ChanneledDataBlockEntity(pos, state);
    }

    protected int getChannel(ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ChannelContainer container) {
            return container.channel();
        }
        return 0;
    }

    public static abstract class Model extends BaseCableModel {
        protected final ItemDisplayElement base;

        protected Model(BlockState state) {
            super(state);
            this.base = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.base.setScale(new Vector3f(2));

            updateStatePos(state);
            this.addElement(this.base);
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
