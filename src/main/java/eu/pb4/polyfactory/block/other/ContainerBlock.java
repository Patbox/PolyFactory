package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.AttackableBlock;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.SneakBypassingBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import xyz.nucleoid.packettweaker.PacketContext;


public class ContainerBlock extends Block implements FactoryBlock, BlockEntityProvider, AttackableBlock, SneakBypassingBlock, BarrierBasedWaterloggable {
    public static EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public final int maxStackCount;

    public ContainerBlock(int maxStackCount, Settings settings) {
        super(settings);
        this.maxStackCount = maxStackCount;
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
        builder.add(WATERLOGGED);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        tickWater(state, world, tickView, pos);
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof ContainerBlockEntity be && hit.getSide() == state.get(FACING)) {
            if (be.checkUnlocked(player)) {
                var stack = player.getStackInHand(Hand.MAIN_HAND);

                if (player.isSneaking() && stack.isEmpty() && !be.getItemStack().isEmpty()) {
                    var inv = player.getInventory().getMainStacks();
                    for (int i = 0; i < inv.size(); i++) {
                        var curr = inv.get(i);
                        if (be.matches(curr)) {
                            curr.decrement(be.addItems(curr.getCount()));
                            if (curr.isEmpty()) {
                                inv.set(i, ItemStack.EMPTY);
                            }
                            if (curr.getCount() > 0) {
                                break;
                            }
                        }
                    }
                } else if (stack.isEmpty()) {
                    return ActionResult.FAIL;
                } else {
                    if (player instanceof ServerPlayerEntity serverPlayer) {
                        TriggerCriterion.trigger(serverPlayer, FactoryTriggers.CONTAINER_ADD_ITEM);
                    }
                    var count = player.isSneaking() ? stack.getCount() : 1;
                    if (be.getItemStack().isEmpty()) {
                        be.setItemStack(stack);
                        stack.decrement(be.addItems(count));
                    } else if (be.matches(stack)) {
                        stack.decrement(be.addItems(count));
                    }

                    if (stack.isEmpty()) {
                        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    }
                }
            }
            return ActionResult.SUCCESS_SERVER;
        }

        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    public ActionResult onPlayerAttack(BlockState state, PlayerEntity player, World world, BlockPos pos, Direction direction) {
        if (direction == state.get(FACING)) {
            if (world.getBlockEntity(pos) instanceof ContainerBlockEntity be && be.checkUnlocked(player)) {
                if (!be.isEmpty()) {
                    var stack = be.extract(player.isSneaking() ? be.getItemStack().getMaxCount() : 1);
                    player.getInventory().offerOrDrop(stack);
                } else {
                    be.setItemStack(ItemStack.EMPTY);
                }
            }
            return ActionResult.SUCCESS_SERVER;
        }

        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        world.updateComparators(pos, this);
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ContainerBlockEntity be) {
            return (int) ((be.storage.amount * 15) / be.storage.getCapacity());
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return waterLog(ctx, this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite()));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return FactoryUtil.transform(state, mirror::apply, FACING);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.OAK_PLANKS.getDefaultState();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ContainerBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }

    public final class Model extends BlockModel {
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement mainElement;
        private final ItemDisplayElement itemElement;
        private final TextDisplayElement countElement;

        private Model(ServerWorld world, BlockPos pos, BlockState state) {
            this.mainElement = ItemDisplayElementUtil.createSimple(ContainerBlock.this.asItem());

            this.itemElement = ItemDisplayElementUtil.createSimple();
            this.itemElement.setDisplaySize(1, 1);
            this.itemElement.setItemDisplayContext(ItemDisplayContext.GUI);
            this.itemElement.setViewRange(0.3f);

            this.countElement = new TextDisplayElement(Text.literal("0"));
            this.countElement.setDisplaySize(1, 1);
            this.countElement.setBackground(0);
            this.countElement.setInvisible(true);
            this.countElement.setViewRange(0.2f);

            //this.countElement.setShadow(true);

            this.updateFacing(state);
            this.addElement(this.mainElement);
            this.addElement(this.itemElement);
            this.addElement(this.countElement);
        }

        public void setDisplay(ItemStack stack, long count) {
            this.itemElement.setItem(stack.copy());
            this.countElement.setText(Text.literal("" + count));
        }

        private void updateFacing(BlockState facing) {
            var rot = facing.get(FACING).getRotationQuaternion().mul(Direction.NORTH.getRotationQuaternion());
            mat.clear();
            mat.rotate(rot);
            mat.pushMatrix();
            mat.rotateY(MathHelper.PI);
            mat.scale(2f);
            this.mainElement.setTransformation(mat);
            mat.popMatrix();

            mat.pushMatrix();
            mat.translate(0, 1f / 16f, -6.2f / 16f);
            mat.scale(0.45f, 0.45f, 0.01f);
            this.itemElement.setTransformation(mat);
            mat.popMatrix();


            mat.pushMatrix();
            mat.translate(0, -6.25f / 16f, -6.2f / 16f);
            mat.rotateY(MathHelper.PI);
            mat.scale(0.5f, 0.5f, 0.02f);
            this.countElement.setTransformation(mat);
            mat.popMatrix();

            this.tick();
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.updateFacing(this.blockState());
            }
        }
    }
}
