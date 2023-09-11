package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.models.LodItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class ItemReaderBlock extends DataProviderBlock {
    public static final BooleanProperty POWERED = Properties.POWERED;

    public ItemReaderBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(POWERED, false));
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getPlayerLookDirection().getOpposite()).with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(POWERED);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ItemReaderBlockEntity(pos, state);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory) {
                ItemScatterer.spawn(world, pos, (Inventory) blockEntity);
                world.updateComparators(pos, this);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            boolean powered = state.get(POWERED);
            if (powered != world.isReceivingRedstonePower(pos)) {
                world.setBlockState(pos, state.with(POWERED, !powered), Block.NOTIFY_LISTENERS);

                if (!powered && world.getBlockEntity(pos) instanceof ItemReaderBlockEntity be) {
                    sendData(world, pos, be.nextPage());
                }
            }

        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!player.isSneaking() && state.get(FACING).getOpposite() != hit.getSide() && player instanceof ServerPlayerEntity serverPlayer && hand == Hand.MAIN_HAND && world.getBlockEntity(pos) instanceof ItemReaderBlockEntity be) {
            be.openGui(serverPlayer);
            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    public static class Model extends BaseModel {
        private final LodItemDisplayElement base;
        private final LodItemDisplayElement book;

        private Model(BlockState state) {
            this.base = LodItemDisplayElement.createSimple(state.getBlock().asItem());
            this.base.setScale(new Vector3f(2));

            this.book = LodItemDisplayElement.createSimple();
            this.book.setScale(new Vector3f(0.5f));
            this.book.setDisplaySize(1, 1);
            this.book.setTranslation(new Vector3f(0, 0, 0.35f));

            updateStatePos(state);
            this.addElement(this.base);
            this.addElement(this.book);
        }

        private void updateStatePos(BlockState state) {
            var dir = state.get(FACING);
            float p = -90;
            float y = 0;

            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.asRotation();
            } else if (dir == Direction.DOWN) {
                p = 90;
            }


            this.base.setYaw(y);
            this.base.setPitch(p);
            this.book.setYaw(y);
            this.book.setPitch(p);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(BlockBoundAttachment.get(this).getBlockState());
                this.base.tick();
                this.book.tick();
            }
        }

        public void setItem(ItemStack stack) {
            this.book.setItem(stack);
            this.book.tick();
        }
    }
}
