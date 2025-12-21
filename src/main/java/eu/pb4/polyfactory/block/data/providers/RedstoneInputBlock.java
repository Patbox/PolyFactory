package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.RedstoneConnectable;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.output.RedstoneOutputBlock;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.RedstoneData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class RedstoneInputBlock extends DirectionalCabledDataProviderBlock implements RedstoneConnectable {
    public static final IntegerProperty POWER = RedstoneOutputBlock.POWER;

    public RedstoneInputBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER);
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return super.getStateForPlacement(ctx).setValue(POWER, clamp(ctx.getLevel().getBestNeighborSignal(ctx.getClickedPos())));
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        sendData((ServerLevel) world, pos, new RedstoneData(state.getValue(POWER)));
    }

    private int clamp(int receivedRedstonePower) {
        return Mth.clamp(receivedRedstonePower, 0, 15);
    }

    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block sourceBlock, @Nullable Orientation wireOrientation, boolean notify) {
        if (!world.isClientSide()) {
            var power = state.getValue(POWER);
            var dir = state.getValue(FACING);
            var input = clamp(world.getSignal(pos.relative(dir), dir));
            if (power != input) {
                world.setBlock(pos, state.setValue(POWER, input), Block.UPDATE_CLIENTS);
                sendData((ServerLevel) world, pos, new RedstoneData(input));
            }
        }
    }

    public static int sendData(LevelReader world, BlockPos selfPos, DataContainer data) {
        var i = DataProvider.sendData(world, selfPos, data);
        if (i > 0 && FactoryUtil.getClosestPlayer((Level) world, selfPos, 32) instanceof ServerPlayer player) {
            TriggerCriterion.trigger(player, FactoryTriggers.REDSTONE_IN);
        }
        return i;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new RedstoneOutputBlock.Model(initialBlockState);
    }

    @Override
    public boolean canRedstoneConnect(BlockState state, @Nullable Direction dir) {
        return state.getValue(FACING).getOpposite() == dir;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }
}
