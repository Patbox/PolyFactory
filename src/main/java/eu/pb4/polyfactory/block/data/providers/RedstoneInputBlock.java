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
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class RedstoneInputBlock extends DirectionalCabledDataProviderBlock implements RedstoneConnectable {
    public static final IntProperty POWER = RedstoneOutputBlock.POWER;

    public RedstoneInputBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(POWER, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(POWER);
    }

    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(POWER, clamp(ctx.getWorld().getReceivedRedstonePower(ctx.getBlockPos())));
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        sendData((ServerWorld) world, pos, new RedstoneData(state.get(POWER)));
    }

    private int clamp(int receivedRedstonePower) {
        return MathHelper.clamp(receivedRedstonePower, 0, 15);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, @Nullable WireOrientation wireOrientation, boolean notify) {
        if (!world.isClient()) {
            var power = state.get(POWER);
            var dir = state.get(FACING);
            var input = clamp(world.getEmittedRedstonePower(pos.offset(dir), dir));
            if (power != input) {
                world.setBlockState(pos, state.with(POWER, input), Block.NOTIFY_LISTENERS);
                sendData((ServerWorld) world, pos, new RedstoneData(input));
            }
        }
    }

    public static int sendData(WorldView world, BlockPos selfPos, DataContainer data) {
        var i = DataProvider.sendData(world, selfPos, data);
        if (i > 0 && FactoryUtil.getClosestPlayer((World) world, selfPos, 32) instanceof ServerPlayerEntity player) {
            TriggerCriterion.trigger(player, FactoryTriggers.REDSTONE_IN);
        }
        return i;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new RedstoneOutputBlock.Model(initialBlockState);
    }

    @Override
    public boolean canRedstoneConnect(BlockState state, @Nullable Direction dir) {
        return state.get(FACING).getOpposite() == dir;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }
}
