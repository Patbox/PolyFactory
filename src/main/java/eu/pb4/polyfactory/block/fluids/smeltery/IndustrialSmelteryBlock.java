package eu.pb4.polyfactory.block.fluids.smeltery;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.MultiBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.fluids.FluidOutput;
import eu.pb4.polyfactory.block.fluids.transport.PipeBaseBlock;
import eu.pb4.polyfactory.block.fluids.transport.PipeConnectable;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;

import static eu.pb4.polyfactory.ModInit.id;

public class IndustrialSmelteryBlock extends MultiBlock implements FactoryBlock, EntityBlock, WorldlyContainerHolder, FluidOutput.Getter, PipeConnectable {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty HAS_PIPE = BooleanProperty.create("has_pipe");

    public static final int STEEL_BLOCKS = 3;
    public static final int DEEPSLATE_BRICK_BLOCKS = 22;


    public IndustrialSmelteryBlock(Properties settings) {
        super(3, 3, 3, settings);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false).setValue(HAS_PIPE, false));
    }

    public boolean placeSmeltery(ServerLevel world, BlockPos pos) {
        var list = new ArrayList<BlockState>();
        int steel = 0;
        int deepslate = 0;
        BlockState controller = null;
        var start = pos.offset(-1, -1, -1);
        var end = pos.offset(1, 1, 1);
        for (var blockPos : BlockPos.betweenClosed(start, end)) {
            var state = world.getBlockState(blockPos);
            list.add(state);
            if (state.is(Blocks.DEEPSLATE_BRICKS)) deepslate++;
            else if (state.is(FactoryBlocks.STEEL_BLOCK)) steel++;
            else if (state.is(FactoryBlocks.SMELTERY_CORE) && controller == null) controller = state;
            else if ((state.isAir() && !blockPos.equals(pos)) || !state.isAir()) return false;
        }

        if (steel < STEEL_BLOCKS || deepslate < DEEPSLATE_BRICK_BLOCKS || controller == null) {
            return false;
        }

        var state = FactoryBlocks.SMELTERY.defaultBlockState().setValue(FACING, controller.getValue(BlastFurnaceBlock.FACING));
        for (var blockPos : BlockPos.betweenClosed(start, end)) {
            int x = blockPos.getX() - start.getX();
            int y = blockPos.getY() - start.getY();
            int z = blockPos.getZ() - start.getZ();

            world.setBlockAndUpdate(blockPos, state.setValue(this.partX, x).setValue(this.partY, y).setValue(this.partZ, z));

            if (x == 1 && y == 1 && z == 1 && world.getBlockEntity(blockPos) instanceof IndustrialSmelteryBlockEntity be) {
                be.setPositionedBlocks(list);
            }
        }

        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, LIT, HAS_PIPE);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && world.getBlockEntity(getCenter(state, pos)) instanceof IndustrialSmelteryBlockEntity be) {
            be.openGui((ServerPlayer) player);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        var center = getCenter(state, pos);
        if (world.getBlockEntity(center) instanceof IndustrialSmelteryBlockEntity be) {
            var x = be.getPositionedBlock(pos.subtract(center));
            if (x != null && !x.isAir()) {
                return x.getCloneItemStack(world, pos, false);
            }
        }
        return super.getCloneItemStack(world, pos, state, includeData);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
        if (isCenter(state)) {
            return;
        }
        var center = getCenter(state, pos);
        if (world.getBlockEntity(center) instanceof IndustrialSmelteryBlockEntity be) {
            be.breakSmeltery(world, center, pos, false);
        }
    }


    @Override
    protected boolean canDropStackFrom(BlockState state) {
        return false;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        var hasPipe = false;
        for (var x : Direction.values()) {
            var tmp = world.getBlockState(pos.relative(x));
            hasPipe |= (tmp.getBlock() instanceof PipeBaseBlock block && block.checkModelDirection(tmp, x.getOpposite()))
                    || (tmp.is(FactoryBlocks.FAUCED) && tmp.getValue(FaucedBlock.FACING) == x);
        }

        return state.setValue(HAS_PIPE, hasPipe);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        var part = initialBlockState.getValue(FACING).getAxis() == Direction.Axis.X ? IndustrialSmelteryBlock.this.partZ : IndustrialSmelteryBlock.this.partX;
        if (initialBlockState.getValue(this.partY) == 2 && initialBlockState.getValue(part) != 1) {
            return new ModelTopPipe(initialBlockState);
        }

        return isCenter(initialBlockState) ? new Model(initialBlockState) : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isCenter(state) ? new IndustrialSmelteryBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return isCenter(state) ? IndustrialSmelteryBlockEntity::tick : null;
    }

    @Override
    public WorldlyContainer getContainer(BlockState state, LevelAccessor world, BlockPos pos) {
        var center = this.getCenter(state, pos);
        var be = world.getBlockEntity(center);

        return be instanceof WorldlyContainer inv ? inv : null;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    @Override
    public FluidOutput getFluidOutput(ServerLevel world, BlockPos pos, Direction direction) {
        return (FluidOutput) world.getBlockEntity(getCenter(world.getBlockState(pos), pos));
    }

    @Override
    public boolean canPipeConnect(LevelReader world, BlockPos pos, BlockState state, Direction dir) {
        return true;
    }

    @Override
    public boolean forceLightUpdates(BlockState blockState) {
        return isCenter(blockState);
    }

    public static final class Model extends BlockModel {
        private static final ItemStack REGULAR = ItemDisplayElementUtil.getSolidModel(id("block/smeltery"));
        private static final ItemStack LIT = ItemDisplayElementUtil.getSolidModel(id("block/smeltery_lit"));

        private final ItemDisplayElement main;

        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.getValue(IndustrialSmelteryBlock.LIT) ? LIT : REGULAR);
            this.main.setOffset(new Vec3(0, -1, 0));
            this.main.setScale(new Vector3f(2f * 2));
            this.main.setTranslation(new Vector3f(0, 0.5f, 0));
            this.main.setDisplaySize(5, 5);
            this.updateStatePos(state);
            this.addElement(this.main);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.getValue(FACING);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.toYRot();
            } else if (dir == Direction.DOWN) {
                p = 90;
            }

            this.main.setYaw(y);
            this.main.setPitch(p);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.main.setItem(this.blockState().getValue(IndustrialSmelteryBlock.LIT) ? LIT : REGULAR);
                updateStatePos(this.blockState());
                this.tick();
            }
        }
    }

    public final class ModelTopPipe extends BlockModel {
        private static final ItemStack MODEL = ItemDisplayElementUtil.getSolidModel(id("block/smeltery_top_pipe"));

        private final ItemDisplayElement main;

        private ModelTopPipe(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple();
            this.main.setDisplaySize(2, 2);
            this.updateStatePos(state);
            this.addElement(this.main);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.getValue(FACING);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.toYRot();
            } else if (dir == Direction.DOWN) {
                p = 90;
            }

            this.main.setItem(state.getValue(HAS_PIPE) ? MODEL : ItemStack.EMPTY);
            this.main.setYaw(y - 90);
            this.main.setPitch(p);

            var part = dir.getAxis() == Direction.Axis.X ? IndustrialSmelteryBlock.this.partZ : IndustrialSmelteryBlock.this.partX;
            var val = state.getValue(part);

            this.main.setTranslation(new Vector3f(0, 0.25f, ((val == 0) == (dir.getAxis() == Direction.Axis.X) ? 0.25f : -0.25f) * dir.getAxisDirection().getStep()));
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
                this.tick();
            }
        }
    }
}
