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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fStack;
import xyz.nucleoid.packettweaker.PacketContext;


public class ContainerBlock extends Block implements FactoryBlock, EntityBlock, AttackableBlock, SneakBypassingBlock, BarrierBasedWaterloggable {
    public static EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public final int maxStackCount;

    public ContainerBlock(int maxStackCount, Properties settings) {
        super(settings);
        this.maxStackCount = maxStackCount;
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
        builder.add(WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof ContainerBlockEntity be && hit.getDirection() == state.getValue(FACING)) {
            if (be.checkUnlocked(player)) {
                var stack = player.getItemInHand(InteractionHand.MAIN_HAND);

                if (player.isShiftKeyDown() && stack.isEmpty() && !be.getItemStack().isEmpty()) {
                    var inv = player.getInventory().getNonEquipmentItems();
                    for (int i = 0; i < inv.size(); i++) {
                        var curr = inv.get(i);
                        if (be.matches(curr)) {
                            curr.shrink(be.addItems(curr.getCount()));
                            if (curr.isEmpty()) {
                                inv.set(i, ItemStack.EMPTY);
                            }
                            if (curr.getCount() > 0) {
                                break;
                            }
                        }
                    }
                } else if (stack.isEmpty()) {
                    return InteractionResult.FAIL;
                } else {
                    if (player instanceof ServerPlayer serverPlayer) {
                        TriggerCriterion.trigger(serverPlayer, FactoryTriggers.CONTAINER_ADD_ITEM);
                    }
                    var count = player.isShiftKeyDown() ? stack.getCount() : 1;
                    if (be.getItemStack().isEmpty()) {
                        be.setItemStack(stack);
                        stack.shrink(be.addItems(count));
                    } else if (be.matches(stack)) {
                        stack.shrink(be.addItems(count));
                    }

                    if (stack.isEmpty()) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    }
                }
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    public InteractionResult onPlayerAttack(BlockState state, Player player, Level world, BlockPos pos, Direction direction) {
        if (direction == state.getValue(FACING)) {
            if (world.getBlockEntity(pos) instanceof ContainerBlockEntity be && be.checkUnlocked(player)) {
                if (!be.isEmpty()) {
                    var stack = be.extract(player.isShiftKeyDown() ? be.getItemStack().getMaxStackSize() : 1);
                    player.getInventory().placeItemBackInInventory(stack);
                } else {
                    be.setItemStack(ItemStack.EMPTY);
                }
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        world.updateNeighbourForOutputSignal(pos, this);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        if (world.getBlockEntity(pos) instanceof ContainerBlockEntity be) {
            return (int) ((be.storage.amount * 15) / be.storage.getCapacity());
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()));
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
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
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.OAK_PLANKS.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ContainerBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }

    public final class Model extends BlockModel {
        private final Matrix4fStack mat = new Matrix4fStack(2);
        private final ItemDisplayElement mainElement;
        private final ItemDisplayElement itemElement;
        private final TextDisplayElement countElement;

        private Model(ServerLevel world, BlockPos pos, BlockState state) {
            this.mainElement = ItemDisplayElementUtil.createSolid(ContainerBlock.this.asItem());

            this.itemElement = ItemDisplayElementUtil.createSimple();
            this.itemElement.setDisplaySize(1, 1);
            this.itemElement.setItemDisplayContext(ItemDisplayContext.GUI);
            this.itemElement.setViewRange(0.3f);

            this.countElement = new TextDisplayElement(Component.literal("0"));
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
            this.countElement.setText(Component.literal("" + count));
        }

        private void updateFacing(BlockState facing) {
            var rot = facing.getValue(FACING).getRotation().mul(Direction.NORTH.getRotation());
            mat.clear();
            mat.rotate(rot);
            mat.pushMatrix();
            mat.rotateY(Mth.PI);
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
            mat.rotateY(Mth.PI);
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
