package eu.pb4.polyfactory.block.data;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.util.BaseCabledDataBlock;
import eu.pb4.polyfactory.block.data.util.DirectionalCabledDataBlock;
import eu.pb4.polyfactory.block.data.util.OrientableCabledDataBlock;
import eu.pb4.polyfactory.item.block.CabledBlockItem;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.BlockStateNameProvider;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public final class CableBlock extends AbstractCableBlock implements FactoryBlock, BlockStateNameProvider {

    public CableBlock(Properties settings) {
        super(settings);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return (context.getItemInHand().getItem() instanceof CabledBlockItem) || super.canBeReplaced(state, context);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof WallBlock wallBlock) {
            var convert = WallWithCableBlock.MAP.get(wallBlock);
            if (convert != null) {
                stack.consume(1, player);
                var convertState = Objects.requireNonNull(convert.getStateForPlacement(new BlockPlaceContext(world, player, hand, stack, hit)))
                        .setValue(WallWithCableBlock.NORTH_SHAPE, WallWithCableBlock.Side.NONE.cable(state.getValue(NORTH)))
                        .setValue(WallWithCableBlock.SOUTH_SHAPE, WallWithCableBlock.Side.NONE.cable(state.getValue(SOUTH)))
                        .setValue(WallWithCableBlock.WEST_SHAPE, WallWithCableBlock.Side.NONE.cable(state.getValue(WEST)))
                        .setValue(WallWithCableBlock.EAST_SHAPE, WallWithCableBlock.Side.NONE.cable(state.getValue(EAST)));

                world.setBlockAndUpdate(pos, convertState);
                return InteractionResult.SUCCESS_SERVER;
            }
        }

        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected boolean isDirectionBlocked(BlockState state, Direction direction) {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var state = ctx.getLevel().getBlockState(ctx.getClickedPos());
        if (state.getBlock() instanceof BaseCabledDataBlock && !state.getValue(BaseCabledDataBlock.HAS_CABLE)) {
            return state.setValue(BaseCabledDataBlock.HAS_CABLE, true);
        }

        return super.getStateForPlacement(ctx);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.STRUCTURE_VOID.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public Component getName(ServerLevel world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (this == FactoryBlocks.CABLE && blockEntity instanceof ColorProvider be && !be.isDefaultColor()) {
            if (!DyeColorExtra.hasLang(be.getColor())) {
                return Component.translatable("block.polyfactory.cable.colored.full",
                        ColoredItem.getColorName(be.getColor()), ColoredItem.getHexName(be.getColor()));
            } else {
                return Component.translatable("block.polyfactory.cable.colored", ColoredItem.getColorName(be.getColor()));
            }
        }
        return this.getName();
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new BaseCableModel(initialBlockState);
    }
}
