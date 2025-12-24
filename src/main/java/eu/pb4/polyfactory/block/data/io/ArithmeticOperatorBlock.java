package eu.pb4.polyfactory.block.data.io;

import com.google.common.collect.ImmutableList;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.data.DoubleInputTransformerBlock;
import eu.pb4.polyfactory.block.data.DoubleInputTransformerBlockEntity;
import eu.pb4.polyfactory.data.*;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.util.FactoryUtil;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class ArithmeticOperatorBlock extends DoubleInputTransformerBlock {
    public static final EnumProperty<Operation> OPERATION = EnumProperty.create("operation", Operation.class);
    public static final EnumProperty<BasicDataType> MODE = EnumProperty.create("mode", BasicDataType.class);

    public static final List<BlockConfig<?>> WRENCH_ACTIONS = ImmutableList.<BlockConfig<?>>builder()
            .addAll(DoubleInputTransformerBlock.WRENCH_ACTIONS)
            .add(BlockConfig.of("operation", OPERATION, (t, world, pos, side, state) -> Component.translatable("item.polyfactory.wrench.action.operation.arithmetic." + t.getSerializedName())))
            .add(BlockConfig.of("mode", MODE, (basicDataType, world, pos, side, state) -> basicDataType.text()))
            .build();

    public ArithmeticOperatorBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(OPERATION, Operation.ADDITION).setValue(MODE, BasicDataType.INTEGER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPERATION, MODE);
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);

        if (state.is(oldState.getBlock()) && (state.getValue(MODE) != oldState.getValue(MODE) || state.getValue(OPERATION) != oldState.getValue(OPERATION))
                && world instanceof ServerLevel serverWorld && world.getBlockEntity(pos) instanceof DoubleInputTransformerBlockEntity be) {
            sendData(world, state.getValue(FACING_OUTPUT), pos, this.transformData(be.lastInput1(), be.lastInput2(), serverWorld, pos, state, be), 0);
        }
    }

    @Override
    protected DataContainer transformData(DataContainer input1, DataContainer input2, ServerLevel world, BlockPos selfPos, BlockState selfState, DoubleInputTransformerBlockEntity be) {
        var action = selfState.getValue(OPERATION);
        var mode = selfState.getValue(MODE);

        if (FactoryUtil.getClosestPlayer(world, selfPos, 16) instanceof ServerPlayer player) {
            TriggerCriterion.trigger(player, FactoryTriggers.ARITHMETIC_OPERATOR);
        }

        return switch (action) {
            case ADDITION -> mode.add(input1, input2);
            case SUBTRACTION -> mode.subtract(input1, input2);
            case MULTIPLICATION -> mode.multiply(input1, input2);
            case DIVISION -> mode.divide(input1, input2);
            case MODULO -> mode.modulo(input1, input2);
        };
    }

    public enum Operation implements StringRepresentable {
        ADDITION,
        SUBTRACTION,
        MULTIPLICATION,
        DIVISION,
        MODULO
        ;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return WRENCH_ACTIONS;
    }
}
