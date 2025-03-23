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
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.List;
import java.util.function.IntPredicate;

public class DataComparatorBlock extends DoubleInputTransformerBlock {
    public static final EnumProperty<Operation> OPERATION = EnumProperty.of("operation", Operation.class);
    public static final BooleanProperty STRICT = BooleanProperty.of("strict");

    public static final List<BlockConfig<?>> WRENCH_ACTIONS = ImmutableList.<BlockConfig<?>>builder()
            .addAll(DoubleInputTransformerBlock.WRENCH_ACTIONS)
            .add(BlockConfig.of("operation", OPERATION, (t, world, pos, side, state) -> t.text))
            .add(BlockConfig.of("strict", STRICT))
            .build();

    public DataComparatorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(OPERATION, Operation.EQUAL).with(STRICT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(OPERATION);
        builder.add(STRICT);
    }

    @Override
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        if (state.get(FACING_OUTPUT) == direction.getOpposite() && world.getBlockEntity(pos) instanceof DoubleInputTransformerBlockEntity be) {
            return be.lastOutput().isTrue() ? 15 : 0;
        }
        return 0;
    }

    @Override
    protected boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        if (state.isOf(oldState.getBlock()) && (state.get(OPERATION) != oldState.get(OPERATION))
                && world instanceof ServerWorld serverWorld && world.getBlockEntity(pos) instanceof DoubleInputTransformerBlockEntity be) {
            sendData(world, state.get(FACING_OUTPUT), pos, this.transformData(be.lastInput1(), be.lastInput2(), serverWorld, pos, state, be));
        } else {
            world.updateNeighborsAlways(pos, this, null);
        }
    }

    @Override
    public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        super.onStateReplaced(state, world, pos,  moved);

    }

    @Override
    public int sendData(WorldAccess world, Direction direction, BlockPos selfPos, DataContainer data) {
        var i = super.sendData(world, direction, selfPos, data);
        if (world instanceof World w) {
            w.updateNeighborsAlways(selfPos, this, null);
        }
        return i;
    }

    @Override
    protected DataContainer transformData(DataContainer input1, DataContainer input2, ServerWorld world, BlockPos selfPos, BlockState selfState, DoubleInputTransformerBlockEntity be) {
        var action = selfState.get(OPERATION);
        var strict = selfState.get(STRICT);

        if (FactoryUtil.getClosestPlayer(world, selfPos, 16) instanceof ServerPlayerEntity player) {
            TriggerCriterion.trigger(player, FactoryTriggers.DATA_COMPARATOR);
        }

        if (input1.getClass() == input2.getClass() && (action == Operation.EQUAL || action == Operation.NOT_EQUAL)) {
            return BoolData.of(input1.equals(input2) == (action == Operation.EQUAL));
        }

        if (strict && input1.type() != input2.type()) {
            return new InvalidData("mismatched input");
        }

        return BoolData.of(action.predicate.test(input1.compareTo(input2)));
    }

    public enum Operation implements StringIdentifiable {
        EQUAL("equal", i -> i == 0),
        NOT_EQUAL("not_equal", i -> i != 0),
        LESS_THAN("less_than", i -> i < 0),
        LESS_OR_EQUAL("less_or_equal", i -> i <= 0),
        MORE_THAN("more_than", i -> i > 0),
        MORE_OR_EQUAL("more_or_equal", i -> i >= 0)
        ;

        private final String name;
        private final IntPredicate predicate;
        public final Text text;

        Operation(String name, IntPredicate predicate) {
            this.name = name;
            this.predicate = predicate;
            this.text = Text.translatable("item.polyfactory.wrench.action.operation.comparator." + name);
        }

        @Override
        public String asString() {
            return name;
        }
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return WRENCH_ACTIONS;
    }
}
