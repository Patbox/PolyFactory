package eu.pb4.polyfactory.block.fluids;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.MultiBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.fluids.transport.PipeConnectable;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;

import static eu.pb4.polyfactory.ModInit.id;

public class SmelteryBlock extends MultiBlock implements FactoryBlock, BlockEntityProvider, InventoryProvider, FluidOutput.Getter, PipeConnectable {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = Properties.LIT;

    public static final int STEEL_BLOCKS = 3;
    public static final int DEEPSLATE_BRICK_BLOCKS = 22;


    public SmelteryBlock(Settings settings) {
        super(3, 3, 3, settings);
        this.setDefaultState(this.getDefaultState().with(LIT, false));
    }

    public boolean placeSmeltery(ServerWorld world, BlockPos pos) {
        var list = new ArrayList<BlockState>();
        int steel = 0;
        int deepslate = 0;
        BlockState controller = null;
        var start = pos.add(-1, -1, -1);
        var end = pos.add(1, 1, 1);
        for (var blockPos : BlockPos.iterate(start, end)) {
            var state = world.getBlockState(blockPos);
            list.add(state);
            if (state.isOf(Blocks.DEEPSLATE_BRICKS)) deepslate++;
            else if (state.isOf(FactoryBlocks.STEEL_BLOCK)) steel++;
            else if (state.isOf(FactoryBlocks.SMELTERY_CORE) && controller == null) controller = state;
            else if ((state.isAir() && !blockPos.equals(pos)) || !state.isAir()) return false;
        }

        if (steel < STEEL_BLOCKS || deepslate < DEEPSLATE_BRICK_BLOCKS || controller == null) {
            return false;
        }

        var state = FactoryBlocks.SMELTERY.getDefaultState().with(FACING, controller.get(BlastFurnaceBlock.FACING));
        for (var blockPos : BlockPos.iterate(start, end)) {
            int x = blockPos.getX() - start.getX();
            int y = blockPos.getY() - start.getY();
            int z = blockPos.getZ() - start.getZ();

            world.setBlockState(blockPos, state.with(this.partX, x).with(this.partY, y).with(this.partZ, z));

            if (x == 1 && y == 1 && z == 1 && world.getBlockEntity(blockPos) instanceof SmelteryBlockEntity be) {
                be.setPositionedBlocks(list);
            }
        }

        return true;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, LIT);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!player.isSneaking() && world.getBlockEntity(getCenter(state, pos)) instanceof SmelteryBlockEntity be) {
            be.openGui((ServerPlayerEntity) player);
            return ActionResult.SUCCESS_SERVER;
        }

        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        super.onStateReplaced(state, world, pos, moved);
        if (isCenter(state)) {
            return;
        }
        var center = getCenter(state, pos);
        if (world.getBlockEntity(center) instanceof SmelteryBlockEntity be) {
            be.breakSmeltery(world, center, pos, false);
        }
    }


    @Override
    protected boolean canDropStackFrom(BlockState state) {
        return false;
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        return state;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return isCenter(initialBlockState) ? new Model(initialBlockState) : null;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return isCenter(state) ? new SmelteryBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return isCenter(state) ? SmelteryBlockEntity::tick : null;
    }

    @Override
    public SidedInventory getInventory(BlockState state, WorldAccess world, BlockPos pos) {
        var center = this.getCenter(state, pos);
        var be = world.getBlockEntity(center);

        return be instanceof SidedInventory inv ? inv : null;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.DEEPSLATE_BRICKS.getDefaultState();
    }

    @Override
    public FluidOutput getFluidOutput(ServerWorld world, BlockPos pos, Direction direction) {
        return (FluidOutput) world.getBlockEntity(getCenter(world.getBlockState(pos), pos));
    }

    @Override
    public boolean canPipeConnect(WorldView world, BlockPos pos, BlockState state, Direction dir) {
        return true;
    }

    public static final class Model extends BlockModel {
        private static final ItemStack REGULAR = ItemDisplayElementUtil.getModel(id("block/smeltery"));
        private static final ItemStack LIT = ItemDisplayElementUtil.getModel(id("block/smeltery_lit"));

        private final ItemDisplayElement main;
        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.get(SmelteryBlock.LIT) ? LIT : REGULAR);
            this.main.setOffset(FactoryUtil.HALF_BELOW);
            this.main.setScale(new Vector3f(2f * 2));
            this.main.setDisplaySize(5, 5);
            this.updateStatePos(state);
            this.addElement(this.main);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.get(FACING);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.getPositiveHorizontalDegrees();
            } else if (dir == Direction.DOWN) {
                p = 90;
            }

            this.main.setYaw(y);
            this.main.setPitch(p);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.main.setItem(this.blockState().get(SmelteryBlock.LIT) ? LIT : REGULAR);
                updateStatePos(this.blockState());
                this.tick();
            }
        }
    }
}
