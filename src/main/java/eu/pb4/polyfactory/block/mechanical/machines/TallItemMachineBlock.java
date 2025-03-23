package eu.pb4.polyfactory.block.mechanical.machines;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polyfactory.util.movingitem.MovingItemProvider;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public abstract class TallItemMachineBlock extends RotationalNetworkBlock implements FactoryBlock, BlockEntityProvider, InventoryProvider, RotationUser, MovingItemConsumer, MovingItemProvider {
    public static final Property<Part> PART = EnumProperty.of("part", Part.class);
    public static final BooleanProperty HAS_CONVEYOR = BooleanProperty.of("has_conveyor");
    public static final Property<Direction> INPUT_FACING = EnumProperty.of("input_facing", Direction.class, x -> x.getAxis() != Direction.Axis.Y);

    public TallItemMachineBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(PART, Part.MAIN).with(HAS_CONVEYOR, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PART, INPUT_FACING, HAS_CONVEYOR);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return state.get(PART) == Part.TOP ? List.of(new FunctionalAxisNode(state.get(INPUT_FACING).rotateYClockwise().getAxis())) : List.of();
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        if (
                !ctx.getWorld().getBlockState(ctx.getBlockPos().up()).canReplace(ItemPlacementContext.offset(ctx, ctx.getBlockPos().up(), Direction.DOWN))
        ) {
            return null;
        }

        return this.getDefaultState().with(INPUT_FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(HAS_CONVEYOR, ctx.getWorld().getBlockState(ctx.getBlockPos().offset(ctx.getHorizontalPlayerFacing())).isOf(FactoryBlocks.CONVEYOR));
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient) {
            BlockPos blockPos = pos.up();
            world.setBlockState(blockPos, state.with(PART, Part.TOP), 3);
            world.updateNeighbors(pos, Blocks.AIR);
            state.updateNeighbors(world, pos, 3);
        }
    }


    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        var part = state.get(PART);

        if (((part == Part.MAIN && direction == Direction.UP)
                || (part == Part.TOP && direction == Direction.DOWN)
        ) && !neighborState.isOf(this)) {
            Rotational.updateRotationalAt(world, pos);
            return Blocks.AIR.getDefaultState();
        } else if (direction == state.get(INPUT_FACING).getOpposite()) {
            return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random).with(HAS_CONVEYOR, neighborState.isOf(FactoryBlocks.CONVEYOR));
        } else {
            return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && !player.isSneaking()) {
            pos = state.get(PART) == Part.MAIN ? pos : pos.down();

            if (world.getBlockEntity(pos) instanceof TallItemMachineBlockEntity be) {
                return be.onUse(state, world, pos, player, hit);
            }
            return ActionResult.FAIL;
        }

        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        super.onStateReplaced(state, world, pos, moved);
        if (state.get(PART) == Part.MAIN) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory) {
                ItemScatterer.spawn(world, pos, (Inventory) blockEntity);
                world.updateComparators(pos, this);
            }
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(PART) == Part.MAIN ? createSourceBlockEntity(pos, state) : null;
    }

    protected abstract BlockEntity createSourceBlockEntity(BlockPos pos, BlockState state);

    @Override
    public SidedInventory getInventory(BlockState state, WorldAccess world, BlockPos pos) {
        pos = switch (state.get(PART)) {
            case MAIN -> pos;
            case TOP -> pos.down();
        };

        var be = world.getBlockEntity(pos);

        return be instanceof SidedInventory sidedInventory ? sidedInventory : null;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, INPUT_FACING);
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return FactoryUtil.transform(state, mirror::apply, INPUT_FACING);
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return initialBlockState.get(PART) == Part.MAIN ? createModel(world, pos, initialBlockState) : null;
    }

    protected abstract ElementHolder createModel(ServerWorld serverWorld, BlockPos pos, BlockState initialBlockState);

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(state.get(PART) == Part.MAIN ? pos : pos.down()) instanceof TallItemMachineBlockEntity be) {
            modifier.stress(be.getStress());
        }
    }

    public enum Part implements StringIdentifiable {
        MAIN,
        TOP;

        @Override
        public String asString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
