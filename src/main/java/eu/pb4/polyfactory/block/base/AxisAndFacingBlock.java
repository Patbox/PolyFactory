package eu.pb4.polyfactory.block.base;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public abstract class AxisAndFacingBlock extends Block implements ConfigurableBlock, FactoryBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty FIRST_AXIS = FactoryProperties.FIRST_AXIS;
    public static final BlockConfig<?> FIRST_AXIS_CONFIG = BlockConfig.of("axis", FIRST_AXIS, (value, world, pos, side, state) -> Component.literal(getAxis(state).getSerializedName()));

    public AxisAndFacingBlock(Properties settings) {
        super(settings);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var facing = ctx.getNearestLookingDirection().getOpposite();

        var axis = ctx.getClickedFace().getAxis();
        if (axis == facing.getAxis()) {
            axis = getAxis(facing, false);
        }

        return this.defaultBlockState().setValue(FACING, facing).setValue(FIRST_AXIS, (getAxis(facing, true) == axis) != ctx.getPlayer().isShiftKeyDown());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(FIRST_AXIS);
    }

    public static Direction.Axis getAxis(BlockState state) {
        return getAxis(state.getValue(FACING), state.getValue(FIRST_AXIS));
    }

    public static Direction.Axis getAxis(Direction facing, boolean first) {
        return switch (facing.getAxis()) {
            case X -> first ? Direction.Axis.Z : Direction.Axis.Y;
            case Y -> first ? Direction.Axis.X : Direction.Axis.Z;
            case Z -> first ? Direction.Axis.X : Direction.Axis.Y;
        };
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.FACING, FIRST_AXIS_CONFIG);
    }
}
