package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.BaseCabledDataBlock;
import eu.pb4.polyfactory.block.data.util.OrientableCabledDataBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

public class ItemReaderBlock extends OrientableCabledDataProviderBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ItemReaderBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx).setValue(POWERED, ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemReaderBlockEntity(pos, state);
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        world.updateNeighbourForOutputSignal(pos, this);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        if (!world.isClientSide()) {
            boolean powered = state.getValue(POWERED);
            if (powered != world.hasNeighborSignal(pos)) {
                world.setBlock(pos, state.setValue(POWERED, !powered), Block.UPDATE_CLIENTS);

                if (!powered && world.getBlockEntity(pos) instanceof ItemReaderBlockEntity be) {
                    DataProvider.sendData(world, pos, be.nextPage());
                }
            }

        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {

        if (!player.isShiftKeyDown() && getFacing(state).getOpposite() != hit.getDirection() && player instanceof ServerPlayer serverPlayer && world.getBlockEntity(pos) instanceof ItemReaderBlockEntity be) {
            be.openGui(serverPlayer);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new eu.pb4.polyfactory.block.data.providers.ItemReaderBlock.Model(initialBlockState);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
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
            super.updateStatePos(state);
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
