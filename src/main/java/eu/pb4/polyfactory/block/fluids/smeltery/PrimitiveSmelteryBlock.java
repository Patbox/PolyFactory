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
import net.minecraft.util.math.Vec3d;
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

public class PrimitiveSmelteryBlock extends MultiBlock implements FactoryBlock, BlockEntityProvider, InventoryProvider, FluidOutput.Getter, PipeConnectable {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = Properties.LIT;


    public PrimitiveSmelteryBlock(Settings settings) {
        super(1, 2, 1, settings);
        this.setDefaultState(this.getDefaultState().with(LIT, false));
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, LIT);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!player.isSneaking() && world.getBlockEntity(getCenter(state, pos)) instanceof PrimitiveSmelteryBlockEntity be) {
            be.openGui((ServerPlayerEntity) player);
            return ActionResult.SUCCESS_SERVER;
        }

        return super.onUse(state, world, pos, player, hit);
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
        return isCenter(state) ? new PrimitiveSmelteryBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return isCenter(state) ? PrimitiveSmelteryBlockEntity::tick : null;
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
        return Blocks.BRICKS.getDefaultState();
    }

    @Override
    public FluidOutput getFluidOutput(ServerWorld world, BlockPos pos, Direction direction) {
        return (FluidOutput) world.getBlockEntity(getCenter(world.getBlockState(pos), pos));
    }

    @Override
    public boolean canPipeConnect(WorldView world, BlockPos pos, BlockState state, Direction dir) {
        return true;
    }

    @Override
    public boolean forceLightUpdates(BlockState blockState) {
        return isCenter(blockState);
    }

    public static final class Model extends BlockModel {
        private static final ItemStack REGULAR = ItemDisplayElementUtil.getModel(id("block/primitive_smeltery"));
        private static final ItemStack LIT = ItemDisplayElementUtil.getModel(id("block/primitive_smeltery_lit"));

        private final ItemDisplayElement main;

        private Model(BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(state.get(PrimitiveSmelteryBlock.LIT) ? LIT : REGULAR);
            this.main.setScale(new Vector3f(2f));
            this.main.setTranslation(new Vector3f(0, 0.5f, 0));
            this.main.setDisplaySize(3, 3);
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
                this.main.setItem(this.blockState().get(PrimitiveSmelteryBlock.LIT) ? LIT : REGULAR);
                updateStatePos(this.blockState());
                this.tick();
            }
        }
    }
}
