package eu.pb4.polyfactory.block.mechanical.machines;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

public class MinerBlock extends RotationalNetworkBlock implements FactoryBlock, EntityBlock, RotationUser, ConfigurableBlock, BarrierBasedWaterloggable {
    public static final Property<Direction> FACING = BlockStateProperties.FACING;
    private static final List<BlockConfig<?>> WRENCH_ACTIONS = List.of(
            BlockConfig.FACING,
            BlockConfig.ofBlockEntityInt("reach", MinerBlockEntity.class, 1, 2, 0,
                    MinerBlockEntity::reach, MinerBlockEntity::setReach)
    );

    public MinerBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
        builder.add(WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, this.defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection()));
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
        return List.of(new FunctionalDirectionNode(state.getValue(FACING).getOpposite()));
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
        return world.getBlockEntity(pos) instanceof MinerBlockEntity be && be.getStack().isDamageableItem()
                ? (int) (16d * be.getStack().getDamageValue() / be.getStack().getMaxDamage()) : 0;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (placer instanceof Player player && world.getBlockEntity(pos) instanceof MinerBlockEntity be) {
            be.owner = player.getGameProfile();
        }

        super.setPlacedBy(world, pos, state, placer, itemStack);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && world.getBlockEntity(pos) instanceof MinerBlockEntity be) {
            be.openGui((ServerPlayer) player);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
        world.updateNeighbourForOutputSignal(pos, this);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MinerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel && type == FactoryBlockEntities.MINER ? MinerBlockEntity::ticker : null;
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
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof MinerBlockEntity be) {
            modifier.stress(be.getStress());
        }
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return WRENCH_ACTIONS;
    }

    public final class Model extends RotationAwareModel {
        private final ItemDisplayElement item;
        private final ItemDisplayElement main;
        private float rotation = 0;

        private Model(ServerLevel world, BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(FactoryItems.MINER);
            this.item = LodItemDisplayElement.createSimple(ItemStack.EMPTY, 1, 0.5f);

            this.updateAnimation(state.getValue(FACING));
            this.addElement(this.main);
            this.addElement(this.item);
        }

        private void updateAnimation(Direction direction) {
            var mat = mat();
            mat.rotate(direction.getOpposite().getRotation().mul(Direction.NORTH.getRotation()));
            mat.scale(2f);
            this.main.setTransformation(mat);

            mat.rotateY(Mth.HALF_PI);
            mat.scale(0.5f);


            if (this.item.getItem().has(DataComponents.TOOL)) {
                mat.translate(-0.1f, 0.25f, 0);
                mat.translate(-0.25f, -0.25f, 0);
                mat.rotateZ(this.rotation);
                mat.translate(0.25f, 0.25f, 0);
            } else {
                mat.translate(-0.3f, 0, 0);
                mat.rotateZ(this.rotation);
            }

            this.item.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            this.updateAnimation(this.blockState().getValue(FACING));
            this.item.startInterpolationIfDirty();
        }

        public void setItem(ItemStack stack) {
            this.item.setItem(stack);
        }

        public void rotate(float value) {
            this.rotation += value;
            if (this.rotation > Mth.TWO_PI) {
                this.rotation -= Mth.TWO_PI;
            }
        }
    }
}
