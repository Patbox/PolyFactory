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
import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class AxleBlock extends RotationalNetworkBlock implements FactoryBlock, ConfigurableBlock, QuickWaterloggable, PolymerTexturedBlock {
    public static final Property<Direction.Axis> AXIS = Properties.AXIS;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public AxleBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS).add(WATERLOGGED);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        tickWater(state, world, tickView, pos);
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, super.getPlacementState(ctx).with(AXIS, ctx.getSide().getAxis()));
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new SimpleAxisNode(state.get(AXIS)));
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.STRIPPED_OAK_LOG.getDefaultState();
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
       return FactoryUtil.rotateAxis(state, AXIS, rotation);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return (state.get(WATERLOGGED) ? FactoryUtil.LIGHTNING_ROD_WATERLOGGED : FactoryUtil.LIGHTNING_ROD_REGULAR).get(state.get(AXIS));
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.AXIS);
    }


    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    public boolean placesLikeAxle() {
        return true;
    }

    public static final class Model extends RotationAwareModel {
        public static final ItemStack ITEM_MODEL = ItemDisplayElementUtil.getModel(FactoryUtil.id("block/axle"));
        public static final ItemStack ITEM_MODEL_SHORT = ItemDisplayElementUtil.getModel(FactoryUtil.id("block/axle_short"));
        private final ItemDisplayElement mainElement;
        private Model(ServerWorld world, BlockState state) {
            this.mainElement = LodItemDisplayElement.createSimple(ITEM_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.mainElement.setViewRange(0.7f);
            this.mainElement.setTeleportDuration(1);
            this.updateAnimation(0,  state.get(AXIS));
            this.addElement(this.mainElement);
        }

        private void updateAnimation(float rotation, Direction.Axis axis) {
            var mat = mat();
            switch (axis) {
                case X -> mat.rotate(Direction.EAST.getRotationQuaternion());
                case Z -> mat.rotate(Direction.SOUTH.getRotationQuaternion());
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

            var tick = this.blockAware().getWorld().getTime();

            if (tick % this.getUpdateRate() == 0) {
                this.updateAnimation(this.getRotation(), this.blockAware().getBlockState().get(AXIS));
                this.mainElement.startInterpolationIfDirty();
            }
        }
    }
}
