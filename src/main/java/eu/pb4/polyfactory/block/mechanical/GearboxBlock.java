package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.AllSideNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class GearboxBlock extends RotationalNetworkBlock implements FactoryBlock, BarrierBasedWaterloggable {
    public GearboxBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, super.getStateForPlacement(ctx));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new AllSideNode());
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model();
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    public final class Model extends RotationAwareModel {
        private final ItemDisplayElement mainElement;
        private final ItemDisplayElement xAxle;
        private final ItemDisplayElement yAxle;
        private final ItemDisplayElement zAxle;

        private Model() {
            this.mainElement = ItemDisplayElementUtil.createSolid(FactoryItems.GEARBOX);
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
            var mat = mat();
            mat.rotateY(rotation);
            mat.scale(2, 2.005f, 2);
            this.yAxle.setTransformation(mat);

            mat.identity();
            mat.rotate(Direction.EAST.getRotation());
            mat.rotateY(rotation);
            mat.scale(2, 2.005f, 2);
            this.xAxle.setTransformation(mat);

            mat.identity();
            mat.rotate(Direction.SOUTH.getRotation());
            mat.rotateY(rotation);
            mat.scale(2, 2.005f, 2);
            this.zAxle.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getGameTime();

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation());
                this.xAxle.startInterpolationIfDirty();
                this.yAxle.startInterpolationIfDirty();
                this.zAxle.startInterpolationIfDirty();
            }
        }
    }
}
