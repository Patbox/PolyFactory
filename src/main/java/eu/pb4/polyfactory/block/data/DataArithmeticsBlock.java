package eu.pb4.polyfactory.block.data;

import com.google.common.collect.ImmutableList;
import com.kneelawk.graphlib.api.graph.NodeHolder;
import eu.pb4.polyfactory.data.*;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.nodes.DirectionNode;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DataArithmeticsBlock extends DoubleInputTransformerBlock {
    public static final EnumProperty<Action> ACTION = EnumProperty.of("action", Action.class);
    public static final EnumProperty<Mode> MODE = EnumProperty.of("mode", Mode.class);

    public static final List<WrenchAction> WRENCH_ACTIONS = ImmutableList.<WrenchAction>builder()
            .addAll(DoubleInputTransformerBlock.WRENCH_ACTIONS)
            .add(WrenchAction.of("action", ACTION))
            .add(WrenchAction.of("mode", MODE))
            .build();

    public DataArithmeticsBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(ACTION, Action.ADD).with(MODE, Mode.INTEGER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ACTION, MODE);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (state.isOf(newState.getBlock()) && (state.get(MODE) != newState.get(MODE) || state.get(ACTION) != newState.get(ACTION))
                && world instanceof ServerWorld serverWorld && world.getBlockEntity(pos) instanceof DoubleInputTransformerBlockEntity be) {
            sendData(world, newState.get(FACING_OUTPUT), pos, this.transformData(be.lastInput1(), be.lastInput2(), serverWorld, pos, newState, be));
        }
    }

    @Override
    protected DataContainer transformData(DataContainer input1, DataContainer input2, ServerWorld world, BlockPos selfPos, BlockState selfState, DoubleInputTransformerBlockEntity be) {
        var action = selfState.get(ACTION);
        var mode = selfState.get(MODE);

        return switch (action) {
            case ADD -> switch (mode) {
                case INTEGER -> new LongData(input1.asLong() + input2.asLong());
                case DECIMAL -> new DoubleData(input1.asDouble() + input2.asDouble());
                case BOOLEAN -> new BoolData(input1.isTrue() || input2.isTrue());
                case STRING -> StringData.ofLimited(input1.asString() + input2.asString());
            };
            case SUBTRACT -> switch (mode) {
                case INTEGER -> new LongData(input1.asLong() - input2.asLong());
                case DECIMAL -> new DoubleData(input1.asDouble() - input2.asDouble());
                case BOOLEAN -> new BoolData(input1.isTrue() && !input2.isTrue());
                case STRING -> StringData.ofLimited(input1.asString().replace(input2.asString(), ""));
            };
            case MULTIPLY -> switch (mode) {
                case INTEGER -> new LongData(input1.asLong() * input2.asLong());
                case DECIMAL -> new DoubleData(input1.asDouble() * input2.asDouble());
                case BOOLEAN -> new BoolData(input1.isTrue() && input2.isTrue());
                case STRING -> StringData.EMPTY;
            };
            case DIVIDE -> switch (mode) {
                case INTEGER -> input2.asLong() != 0 ? new LongData(input1.asLong() / input2.asLong()) : LongData.ZERO;
                case DECIMAL -> new DoubleData(input1.asDouble() / input2.asDouble());
                case BOOLEAN -> new BoolData(!input1.isTrue() && !input2.isTrue());
                case STRING -> StringData.EMPTY;
            };

            case DIVIDE_REST -> switch (mode) {
                case INTEGER -> input2.asLong() != 0 ? new LongData(input1.asLong() % input2.asLong()) : LongData.ZERO;
                case DECIMAL -> new DoubleData(input1.asDouble() % input2.asDouble());
                case BOOLEAN -> new BoolData(!input1.isTrue() || !input2.isTrue());
                case STRING -> StringData.EMPTY;
            };
        };
    }

    public enum Action implements StringIdentifiable {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        DIVIDE_REST
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
