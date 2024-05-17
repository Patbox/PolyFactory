package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

public class ItemReaderBlock extends CabledDataProviderBlock {
    public static final BooleanProperty POWERED = Properties.POWERED;

    public ItemReaderBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(POWERED, false));
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
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
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        var x = super.onUse(state, world, pos, player, hit);

        if (x == ActionResult.PASS  && !player.isSneaking() && state.get(FACING).getOpposite() != hit.getSide() && player instanceof ServerPlayerEntity serverPlayer && world.getBlockEntity(pos) instanceof ItemReaderBlockEntity be) {
            be.openGui(serverPlayer);
            return ActionResult.SUCCESS;
        }

        return x;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    public static class Model extends BaseCableModel {
        private final ItemDisplayElement base;
        private final ItemDisplayElement book;

        private Model(BlockState state) {
            super(state, state.get(HAS_CABLE));
            this.base = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
            this.base.setScale(new Vector3f(2));

            this.book = ItemDisplayElementUtil.createSimple();
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
        protected void setState(BlockState blockState) {
            super.setState(blockState);
            updateStatePos(this.blockState());
            this.base.tick();
            this.book.tick();
        }

        public void setItem(ItemStack stack) {
            this.book.setItem(stack);
            this.book.tick();
        }
    }
}
