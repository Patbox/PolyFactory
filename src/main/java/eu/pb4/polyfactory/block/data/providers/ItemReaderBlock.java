package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.BaseCabledDataBlock;
import eu.pb4.polyfactory.block.data.util.OrientableCabledDataBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

public class ItemReaderBlock extends OrientableCabledDataProviderBlock {
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
    public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        world.updateComparators(pos, this);
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        if (!world.isClient()) {
            boolean powered = state.get(POWERED);
            if (powered != world.isReceivingRedstonePower(pos)) {
                world.setBlockState(pos, state.with(POWERED, !powered), Block.NOTIFY_LISTENERS);

                if (!powered && world.getBlockEntity(pos) instanceof ItemReaderBlockEntity be) {
                    DataProvider.sendData(world, pos, be.nextPage());
                }
            }

        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {

        if (!player.isSneaking() && getFacing(state).getOpposite() != hit.getSide() && player instanceof ServerPlayerEntity serverPlayer && world.getBlockEntity(pos) instanceof ItemReaderBlockEntity be) {
            be.openGui(serverPlayer);
            return ActionResult.SUCCESS_SERVER;
        }

        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    public class Model extends OrientableCabledDataBlock.Model {
        private final ItemDisplayElement book;

        private Model(BlockState state) {
            super(state);
            this.book = ItemDisplayElementUtil.createSimple();
            this.book.setScale(new Vector3f(0.5f));
            this.book.setDisplaySize(1, 1);
            this.book.setTranslation(new Vector3f(0, 0, 0.35f));
            this.book.setYaw(this.base.getYaw());
            this.book.setPitch(this.base.getPitch());
            this.addElement(this.book);
        }

        @Override
        protected void updateStatePos(BlockState state) {
            if (this.book != null) {
                this.book.setYaw(this.base.getYaw());
                this.book.setPitch(this.base.getPitch());
            }
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
