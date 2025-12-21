package eu.pb4.polyfactory.block.mechanical.machines;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;

import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class PlanterBlock extends RotationalNetworkBlock implements FactoryBlock, EntityBlock, RotationUser, BarrierBasedWaterloggable, ConfigurableBlock {
    private static final BlockConfig RADIUS_ACTION = BlockConfig.ofBlockEntityInt("radius", PlanterBlockEntity.class, 1, 2, 0,
            PlanterBlockEntity::radius, PlanterBlockEntity::setRadius);

    public PlanterBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, this.defaultBlockState());
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }
    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new FunctionalAxisNode(Direction.Axis.Y));
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return world.getBlockEntity(pos) instanceof PlanterBlockEntity be ? AbstractContainerMenu.getRedstoneSignalFromContainer((Container) be) : 0;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (placer instanceof Player player && world.getBlockEntity(pos) instanceof PlanterBlockEntity be) {
            be.owner = player.getGameProfile();
        }

        super.setPlacedBy(world, pos, state, placer, itemStack);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && world.getBlockEntity(pos) instanceof PlanterBlockEntity be) {
            be.openGui((ServerPlayer) player);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        world.updateNeighbourForOutputSignal(pos, this);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlanterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel && type == FactoryBlockEntities.PLANTER ? PlanterBlockEntity::ticker : null;
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof PlanterBlockEntity be) {
            modifier.stress(be.getStress());
        }
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(RADIUS_ACTION);
    }

    public static final class Model extends RotationAwareModel {
        public static final ItemStack OUTPUT_1 = ItemDisplayElementUtil.getModel(id("block/planter_output"));
        public static final ItemStack OUTPUT_2 = ItemDisplayElementUtil.getModel(id("block/planter_output_2"));

        private final ItemDisplayElement output1;
        private final ItemDisplayElement output2;
        private final ItemDisplayElement main;
        private BlockPos target = null;

        private Model(ServerLevel world, BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(FactoryItems.PLANTER);
            this.output1 = LodItemDisplayElement.createSimple(OUTPUT_1, 5, 0.3f);
            this.output2 = LodItemDisplayElement.createSimple(OUTPUT_2, 5, 0.3f);
            this.output1.setViewRange(0.5f);
            this.output2.setViewRange(0.5f);

            this.updateAnimation();
            this.addElement(this.main);
            this.addElement(this.output1);
            this.addElement(this.output2);
        }

        private void updateAnimation() {
            var mat = mat();
            mat.scale(2f);
            this.main.setTransformation(mat);

            mat.rotateY(Mth.HALF_PI);
            mat.scale(0.5f);


            /*if (this.item.getItem().getItem() instanceof ToolItem) {
                mat.translate(-0.1f, 0.25f, 0);
                mat.translate(-0.25f, -0.25f, 0);
                mat.rotateZ(this.rotation);
                mat.translate(0.25f, 0.25f, 0);
            } else {
                mat.translate(-0.3f, 0, 0);
                mat.rotateZ(this.rotation);
            }

            this.item.setTransformation(mat);*/
        }

        @Override
        protected void onTick() {
            //this.updateAnimation(this.blockState().get(FACING));
            this.output1.startInterpolationIfDirty();
            this.output2.startInterpolationIfDirty();
        }

        public void setDirection(BlockPos selfPos, BlockPos target) {
            if (target.equals(this.target)) {
                return;
            }
            this.target = target;
            var offset = target.subtract(selfPos);
            var angle = Math.atan2(-offset.getX(), -offset.getZ());

            var pos = new Vector3f(Mth.clamp(offset.getX(), -1, 1), 0, Mth.clamp(offset.getZ(), -1, 1)).mul(3.016f / 16);

            this.output1.setTranslation(pos);
            this.output2.setTranslation(new Vector3f(pos).add(0, 1 / 32f,0));
            this.output2.setLeftRotation(new Quaternionf().rotateY((float) angle));
        }
    }
}
