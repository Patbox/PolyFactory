package eu.pb4.polyfactory.block.data.io;

import com.google.common.collect.ImmutableList;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.data.DoubleInputTransformerBlock;
import eu.pb4.polyfactory.block.data.DoubleInputTransformerBlockEntity;
import eu.pb4.polyfactory.data.*;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;

public class ArithmeticOperatorBlock extends DoubleInputTransformerBlock {
    public static final EnumProperty<Operation> OPERATION = EnumProperty.of("operation", Operation.class);
    public static final EnumProperty<BasicDataType> MODE = EnumProperty.of("mode", BasicDataType.class);

    public static final List<BlockConfig<?>> WRENCH_ACTIONS = ImmutableList.<BlockConfig<?>>builder()
            .addAll(DoubleInputTransformerBlock.WRENCH_ACTIONS)
            .add(BlockConfig.of("operation", OPERATION, (t, world, pos, side, state) -> Text.translatable("item.polyfactory.wrench.action.operation.arithmetic." + t.asString())))
            .add(BlockConfig.of("mode", MODE, (basicDataType, world, pos, side, state) -> basicDataType.text()))
            .build();

    public ArithmeticOperatorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(OPERATION, Operation.ADDITION).with(MODE, BasicDataType.INTEGER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(OPERATION, MODE);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);

        if (state.isOf(oldState.getBlock()) && (state.get(MODE) != oldState.get(MODE) || state.get(OPERATION) != oldState.get(OPERATION))
                && world instanceof ServerWorld serverWorld && world.getBlockEntity(pos) instanceof DoubleInputTransformerBlockEntity be) {
            sendData(world, state.get(FACING_OUTPUT), pos, this.transformData(be.lastInput1(), be.lastInput2(), serverWorld, pos, state, be));
        }
    }

    @Override
    protected DataContainer transformData(DataContainer input1, DataContainer input2, ServerWorld world, BlockPos selfPos, BlockState selfState, DoubleInputTransformerBlockEntity be) {
        var action = selfState.get(OPERATION);
        var mode = selfState.get(MODE);

        if (FactoryUtil.getClosestPlayer(world, selfPos, 16) instanceof ServerPlayerEntity player) {
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

    public enum Operation implements StringIdentifiable {
        ADDITION,
        SUBTRACTION,
        MULTIPLICATION,
        DIVISION,
        MODULO
        ;

        @Override
        public String asString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return WRENCH_ACTIONS;
    }
}
