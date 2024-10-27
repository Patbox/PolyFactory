package eu.pb4.polyfactory.block.data.io;

import com.google.common.collect.ImmutableList;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.data.DoubleInputTransformerBlock;
import eu.pb4.polyfactory.block.data.DoubleInputTransformerBlockEntity;
import eu.pb4.polyfactory.data.*;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
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
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;

public class ArithmeticOperatorBlock extends DoubleInputTransformerBlock {
    public static final EnumProperty<Operation> OPERATION = EnumProperty.of("operation", Operation.class);
    public static final EnumProperty<Mode> MODE = EnumProperty.of("mode", Mode.class);

    public static final List<WrenchAction> WRENCH_ACTIONS = ImmutableList.<WrenchAction>builder()
            .addAll(DoubleInputTransformerBlock.WRENCH_ACTIONS)
            .add(WrenchAction.of("operation", OPERATION, t -> Text.translatable("item.polyfactory.wrench.action.operation.arithmetic." + t.asString())))
            .add(WrenchAction.of("mode", MODE, t -> Text.translatable("item.polyfactory.wrench.action.mode.arithmetic." + t.asString())))
            .build();

    public ArithmeticOperatorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(OPERATION, Operation.ADDITION).with(MODE, Mode.INTEGER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(OPERATION, MODE);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (state.isOf(newState.getBlock()) && (state.get(MODE) != newState.get(MODE) || state.get(OPERATION) != newState.get(OPERATION))
                && world instanceof ServerWorld serverWorld && world.getBlockEntity(pos) instanceof DoubleInputTransformerBlockEntity be) {
            sendData(world, newState.get(FACING_OUTPUT), pos, this.transformData(be.lastInput1(), be.lastInput2(), serverWorld, pos, newState, be));
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
            case ADDITION -> switch (mode) {
                case INTEGER -> new LongData(input1.asLong() + input2.asLong());
                case DECIMAL -> new DoubleData(input1.asDouble() + input2.asDouble());
                case BOOLEAN -> new BoolData(input1.isTrue() || input2.isTrue());
                case STRING -> StringData.ofLimited(input1.asString() + input2.asString());
            };
            case SUBTRACTION -> switch (mode) {
                case INTEGER -> new LongData(input1.asLong() - input2.asLong());
                case DECIMAL -> new DoubleData(input1.asDouble() - input2.asDouble());
                case BOOLEAN -> new BoolData(input1.isTrue() && !input2.isTrue());
                case STRING -> StringData.ofLimited(input1.asString().replace(input2.asString(), ""));
            };
            case MULTIPLICATION -> switch (mode) {
                case INTEGER -> new LongData(input1.asLong() * input2.asLong());
                case DECIMAL -> new DoubleData(input1.asDouble() * input2.asDouble());
                case BOOLEAN -> new BoolData(input1.isTrue() && input2.isTrue());
                case STRING -> StringData.EMPTY;
            };
            case DIVISION -> switch (mode) {
                case INTEGER -> input2.asLong() != 0 ? new LongData(input1.asLong() / input2.asLong()) : LongData.ZERO;
                case DECIMAL -> new DoubleData(input1.asDouble() / input2.asDouble());
                case BOOLEAN -> new BoolData(!input1.isTrue() && !input2.isTrue());
                case STRING -> StringData.EMPTY;
            };

            case MODULO -> switch (mode) {
                case INTEGER -> input2.asLong() != 0 ? new LongData(input1.asLong() % input2.asLong()) : LongData.ZERO;
                case DECIMAL -> new DoubleData(input1.asDouble() % input2.asDouble());
                case BOOLEAN -> new BoolData(!input1.isTrue() || !input2.isTrue());
                case STRING -> StringData.EMPTY;
            };
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

    public enum Mode implements StringIdentifiable {
        INTEGER,
        DECIMAL,
        BOOLEAN,
        STRING;

        @Override
        public String asString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    @Override
    public List<WrenchAction> getWrenchActions() {
        return WRENCH_ACTIONS;
    }
}
