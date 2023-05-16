package eu.pb4.polyfactory.block.machines;

import com.kneelawk.graphlib.graph.BlockNode;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.nodes.mechanical.DirectionalMechanicalNode;
import eu.pb4.polyfactory.nodes.mechanical.GearboxNode;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class PressBlock extends NetworkBlock implements PolymerBlock, BlockEntityProvider, InventoryProvider {
    public static final Property<Part> PART = EnumProperty.of("part", Part.class);
    public static final BooleanProperty HAS_CONVEYOR = BooleanProperty.of("has_conveyor");
    public static final Property<Direction> INPUT_FACING = DirectionProperty.of("input_facing", x -> x.getAxis() != Direction.Axis.Y);

    public PressBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(PART, Part.MAIN).with(HAS_CONVEYOR, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PART, INPUT_FACING, HAS_CONVEYOR);
    }

    @Override
    public Collection<BlockNode> createNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return state.get(PART) == Part.HEAD ? List.of(
                new GearboxNode()
        ) : List.of();
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.ANVIL;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return switch (state.get(PART)) {
            case MAIN -> Blocks.ANVIL.getDefaultState().with(AnvilBlock.FACING, state.get(INPUT_FACING));
            case CENTER -> Blocks.BARRIER.getDefaultState();
            case HEAD -> Blocks.PISTON.getDefaultState().with(PistonBlock.EXTENDED, true).with(PistonBlock.FACING, Direction.DOWN);
        };
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        if (
                !ctx.getWorld().getBlockState(ctx.getBlockPos().up()).canReplace(ItemPlacementContext.offset(ctx, ctx.getBlockPos().up(), Direction.DOWN))
                        || !ctx.getWorld().getBlockState(ctx.getBlockPos().up().up()).canReplace(ItemPlacementContext.offset(ctx, ctx.getBlockPos().up(), Direction.DOWN))
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
            world.setBlockState(blockPos, state.with(PART, Part.CENTER), 3);
            world.setBlockState(blockPos.up(), state.with(PART, Part.HEAD), 3);
            world.updateNeighbors(pos, Blocks.AIR);
            state.updateNeighbors(world, pos, 3);
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        var part = state.get(PART);

        if (((part == Part.MAIN && direction == Direction.UP)
                || (part == Part.CENTER && direction.getAxis() == Direction.Axis.Y)
                || (part == Part.HEAD && direction == Direction.DOWN)
        ) && !neighborState.isOf(this)) {
            NetworkComponent.updateAt(world, pos);
            return Blocks.AIR.getDefaultState();
        } else if (direction == state.get(INPUT_FACING).getOpposite()) {
            return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos).with(HAS_CONVEYOR, neighborState.isOf(FactoryBlocks.CONVEYOR));
        } else {
            return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand == Hand.MAIN_HAND && !player.isSneaking() && state.get(PART) == Part.MAIN && world.getBlockEntity(pos) instanceof PressBlockEntity be) {
            be.openGui((ServerPlayerEntity) player);
            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (state.get(PART) == Part.MAIN) {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof Inventory) {
                    ItemScatterer.spawn(world, pos, (Inventory) blockEntity);
                    world.updateComparators(pos, this);
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(PART) == Part.MAIN ? new PressBlockEntity(pos, state) : null;
    }

    @Override
    public SidedInventory getInventory(BlockState state, WorldAccess world, BlockPos pos) {
        pos = switch (state.get(PART)) {
            case MAIN -> pos;
            case CENTER -> pos.down();
            case HEAD -> pos.down(2);
        };

        var be = world.getBlockEntity(pos);

        return be instanceof SidedInventory sidedInventory ? sidedInventory : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.PRESS ? PressBlockEntity::ticker : null;
    }

    public enum Part implements StringIdentifiable {
        MAIN,
        CENTER,
        HEAD;

        @Override
        public String asString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
