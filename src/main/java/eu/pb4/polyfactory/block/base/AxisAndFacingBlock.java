package eu.pb4.polyfactory.block.base;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AxisAndFacingBlock extends Block implements ConfigurableBlock, FactoryBlock {
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final BooleanProperty FIRST_AXIS = FactoryProperties.FIRST_AXIS;
    public static final BlockConfig<?> FIRST_AXIS_ACTION = BlockConfig.of("axis", FIRST_AXIS, (value, world, pos, side, state) -> Text.literal(getAxis(state).asString()));

    public AxisAndFacingBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var facing = ctx.getPlayerLookDirection().getOpposite();

        var axis = ctx.getSide().getAxis();
        if (axis == facing.getAxis()) {
            axis = getAxis(facing, false);
        }

        return this.getDefaultState().with(FACING, facing).with(FIRST_AXIS, (getAxis(facing, true) == axis) != ctx.getPlayer().isSneaking());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(FIRST_AXIS);
    }

    public static Direction.Axis getAxis(BlockState state) {
        return getAxis(state.get(FACING), state.get(FIRST_AXIS));
    }

    public static Direction.Axis getAxis(Direction facing, boolean first) {
        return switch (facing.getAxis()) {
            case X -> first ? Direction.Axis.Z : Direction.Axis.Y;
            case Y -> first ? Direction.Axis.X : Direction.Axis.Z;
            case Z -> first ? Direction.Axis.X : Direction.Axis.Y;
        };
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING, FIRST_AXIS_ACTION);
    }
}
