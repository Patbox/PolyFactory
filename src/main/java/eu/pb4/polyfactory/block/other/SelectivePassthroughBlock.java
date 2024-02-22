package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.entity.FactoryEntityTags;
import eu.pb4.factorytools.api.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.tracker.EntityTrackedData;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class SelectivePassthroughBlock extends Block implements FactoryBlock, BarrierBasedWaterloggable {
    public SelectivePassthroughBlock(Settings settings) {
        super(settings.dynamicBounds());
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.IRON_BARS.getDefaultState().with(PaneBlock.NORTH, true).with(PaneBlock.SOUTH, true)
                .with(PaneBlock.EAST, true).with(PaneBlock.WEST, true);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, super.getPlacementState(ctx));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        tickWater(state, world, pos);
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (context instanceof EntityShapeContext entityShapeContext && entityShapeContext.getEntity() != null && entityShapeContext.getEntity().getType().isIn(FactoryEntityTags.GRID_PASSABLE)) {
            return VoxelShapes.empty();
        }

        return super.getCollisionShape(state, world, pos, context);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (entity.getType().isIn(FactoryEntityTags.GRID_PASSABLE)) {
            entity.getDataTracker().set(EntityTrackedData.SILENT, entity.isSilent(), true);
        }
        super.onEntityCollision(state, world, pos, entity);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        var model = new BlockModel();
        var element = LodItemDisplayElement.createSimple(this.asItem());
        element.setScale(new Vector3f(1.9995f));
        model.addElement(element);
        return model;
    }
}
