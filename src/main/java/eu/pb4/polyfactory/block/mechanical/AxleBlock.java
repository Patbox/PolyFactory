package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.QuickWaterloggable;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.SimpleAxisNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.common.impl.CommonImplUtils;
import eu.pb4.polymer.core.impl.networking.PolymerServerProtocol;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class AxleBlock extends RotationalNetworkBlock implements FactoryBlock, ConfigurableBlock, QuickWaterloggable, PolymerTexturedBlock {
    public static final Property<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AxleBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS).add(WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, super.getStateForPlacement(ctx).setValue(AXIS, ctx.getClickedFace().getAxis()));
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new SimpleAxisNode(state.getValue(AXIS)));
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.STRIPPED_OAK_LOG.defaultBlockState();
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
       return FactoryUtil.rotateAxis(state, AXIS, rotation);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return (state.getValue(WATERLOGGED) ? FactoryUtil.LIGHTNING_ROD_WATERLOGGED : FactoryUtil.LIGHTNING_ROD_REGULAR).get(state.getValue(AXIS));
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.AXIS);
    }


    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public boolean placesLikeAxle() {
        return true;
    }

    public static final class Model extends RotationAwareModel {
        public static final ItemStack ITEM_MODEL = ItemDisplayElementUtil.getSolidModel(FactoryUtil.id("block/axle"));
        public static final ItemStack ITEM_MODEL_SHORT = ItemDisplayElementUtil.getSolidModel(FactoryUtil.id("block/axle_short"));
        private final ItemDisplayElement mainElement;
        private Model(ServerLevel world, BlockState state) {
            this.mainElement = LodItemDisplayElement.createSimple(ITEM_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.mainElement.setViewRange(0.7f);
            this.mainElement.setTeleportDuration(1);
            this.updateAnimation(0,  state.getValue(AXIS));
            this.addElement(this.mainElement);
        }

        private void updateAnimation(float rotation, Direction.Axis axis) {
            var mat = mat();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotation());
                case Z -> mat.rotate(Direction.SOUTH.getRotation());
            }

            mat.rotateY(rotation);

            mat.scale(2, 2f, 2);
            this.mainElement.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            if (!this.blockAware().isPartOfTheWorld()) {
                return;
            }

            var tick = this.blockAware().getWorld().getGameTime();

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation(), this.blockAware().getBlockState().getValue(AXIS));
                this.mainElement.startInterpolationIfDirty();
            }
        }
    }
}
