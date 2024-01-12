package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.AllSideNode;
import eu.pb4.factorytools.api.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

public class GearboxBlock extends RotationalNetworkBlock implements FactoryBlock, BarrierBasedWaterloggable {
    public GearboxBlock(Settings settings) {
        super(settings);
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
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new AllSideNode());
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.SMOOTH_STONE.getDefaultState();
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model();
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    public final class Model extends RotationAwareModel {
        private final ItemDisplayElement mainElement;
        private final ItemDisplayElement xAxle;
        private final ItemDisplayElement yAxle;
        private final ItemDisplayElement zAxle;

        private Model() {
            this.mainElement = LodItemDisplayElement.createSimple(FactoryItems.GEARBOX);
            this.mainElement.setScale(new Vector3f(2));
            this.addElement(this.mainElement);

            this.xAxle = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.yAxle = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.zAxle = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.xAxle.setViewRange(0.5f);
            this.yAxle.setViewRange(0.5f);
            this.zAxle.setViewRange(0.5f);

            this.updateAnimation(0);

            this.addElement(this.mainElement);
            this.addElement(this.xAxle);
            this.addElement(this.yAxle);
            this.addElement(this.zAxle);
        }

        private void updateAnimation(float rotation) {
            mat.identity();
            mat.rotateY(rotation);
            mat.scale(2, 2.005f, 2);
            this.yAxle.setTransformation(mat);

            mat.identity();
            mat.rotate(Direction.EAST.getRotationQuaternion());
            mat.rotateY(rotation);
            mat.scale(2, 2.005f, 2);
            this.xAxle.setTransformation(mat);

            mat.identity();
            mat.rotate(Direction.SOUTH.getRotationQuaternion());
            mat.rotateY(rotation);
            mat.scale(2, 2.005f, 2);
            this.zAxle.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation());
                if (this.xAxle.isDirty()) {
                    this.xAxle.startInterpolation();
                    this.yAxle.startInterpolation();
                    this.zAxle.startInterpolation();
                }
            }
        }
    }
}
