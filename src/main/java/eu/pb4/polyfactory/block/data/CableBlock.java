package eu.pb4.polyfactory.block.data;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.util.DirectionalCabledDataBlock;
import eu.pb4.polyfactory.item.block.CabledBlockItem;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.BlockStateNameProvider;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Objects;

public final class CableBlock extends AbstractCableBlock implements FactoryBlock, BlockStateNameProvider {

    public CableBlock(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        return (context.getStack().getItem() instanceof CabledBlockItem) || super.canReplace(state, context);
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof WallBlock wallBlock) {
            var convert = WallWithCableBlock.MAP.get(wallBlock);
            if (convert != null) {
                stack.decrementUnlessCreative(1, player);
                var convertState = Objects.requireNonNull(convert.getPlacementState(new ItemPlacementContext(world, player, hand, stack, hit)))
                        .with(WallWithCableBlock.NORTH_SHAPE, WallWithCableBlock.Side.NONE.cable(state.get(NORTH)))
                        .with(WallWithCableBlock.SOUTH_SHAPE, WallWithCableBlock.Side.NONE.cable(state.get(SOUTH)))
                        .with(WallWithCableBlock.WEST_SHAPE, WallWithCableBlock.Side.NONE.cable(state.get(WEST)))
                        .with(WallWithCableBlock.EAST_SHAPE, WallWithCableBlock.Side.NONE.cable(state.get(EAST)));

                world.setBlockState(pos, convertState);
                return ActionResult.SUCCESS_SERVER;
            }
        }

        return super.onUseWithItem(stack, state, world, pos, player, hand, hit);
    }

    @Override
    protected boolean isDirectionBlocked(BlockState state, Direction direction) {
        return false;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var state = ctx.getWorld().getBlockState(ctx.getBlockPos());
        if (state.getBlock() instanceof DirectionalCabledDataBlock && !state.get(DirectionalCabledDataBlock.HAS_CABLE)) {
            return state.with(DirectionalCabledDataBlock.HAS_CABLE, true);
        } else if (state.getBlock() instanceof DirectionalCabledDataBlock && !state.get(DirectionalCabledDataBlock.HAS_CABLE)) {
            return state.with(DirectionalCabledDataBlock.HAS_CABLE, true);
        }

        return super.getPlacementState(ctx);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.STRUCTURE_VOID.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public Text getName(ServerWorld world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (this == FactoryBlocks.CABLE && blockEntity instanceof ColorProvider be && !be.isDefaultColor()) {
            if (!DyeColorExtra.hasLang(be.getColor())) {
                return Text.translatable("block.polyfactory.cable.colored.full",
                        ColoredItem.getColorName(be.getColor()), ColoredItem.getHexName(be.getColor()));
            } else {
                return Text.translatable("block.polyfactory.cable.colored", ColoredItem.getColorName(be.getColor()));
            }
        }
        return this.getName();
    }


    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new BaseCableModel(initialBlockState);
    }
}
