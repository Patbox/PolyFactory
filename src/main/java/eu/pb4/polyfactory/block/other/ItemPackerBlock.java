package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;


public class ItemPackerBlock extends Block implements FactoryBlock, EntityBlock, BarrierBasedWaterloggable, WorldlyContainerHolder, ConfigurableBlock {
    public static EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public ItemPackerBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
        builder.add(WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof ItemPackerBlockEntity be && player instanceof ServerPlayer serverPlayer) {
            be.openGui(serverPlayer);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        world.updateNeighbourForOutputSignal(pos, this);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof ItemPackerBlockEntity be) {
            return be.getComparatorOutput();
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, this.defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite()));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FactoryUtil.transform(state, mirror::mirror, FACING);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPackerBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }

    @Override
    public WorldlyContainer getContainer(BlockState state, LevelAccessor world, BlockPos pos) {
        return null;
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING);
    }

    public final class Model extends BlockModel {
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement mainElement;
        private final ItemDisplayElement itemElement;

        private Model(ServerLevel world, BlockPos pos, BlockState state) {
            this.mainElement = ItemDisplayElementUtil.createSolid(ItemPackerBlock.this.asItem());

            this.itemElement = ItemDisplayElementUtil.createSimple();
            this.itemElement.setDisplaySize(1, 1);
            this.itemElement.setItemDisplayContext(ItemDisplayContext.NONE);
            this.itemElement.setViewRange(0.4f);

            //this.countElement.setShadow(true);

            this.updateFacing(state);
            this.addElement(this.mainElement);
            this.addElement(this.itemElement);
        }

        public void setDisplay(ItemStack stack) {
            this.itemElement.setItem(stack.copy());
            this.tick();
        }

        private void updateFacing(BlockState facing) {
            var rot = facing.getValue(FACING).getRotation().mul(Direction.NORTH.getRotation());
            mat.clear();
            mat.rotate(rot);
            mat.pushMatrix();
            mat.rotateY(Mth.PI);
            mat.scale(2f);
            this.mainElement.setTransformation(mat);
            mat.popMatrix();

            mat.pushMatrix();
            //mat.translate(0, 1f / 16f, -6.2f / 16f);
            mat.scale(10 / 16f);
            this.itemElement.setTransformation(mat);
            mat.popMatrix();
            this.tick();
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.updateFacing(this.blockState());
            }
        }
    }
}
