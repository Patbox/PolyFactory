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
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class DataComparatorBlock extends DoubleInputTransformerBlock {
    public static final EnumProperty<Operation> OPERATION = EnumProperty.create("operation", Operation.class);
    public static final BooleanProperty STRICT = BooleanProperty.create("strict");

    public static final List<BlockConfig<?>> WRENCH_ACTIONS = ImmutableList.<BlockConfig<?>>builder()
            .addAll(DoubleInputTransformerBlock.WRENCH_ACTIONS)
            .add(BlockConfig.of("operation", OPERATION, (t, world, pos, side, state) -> t.text))
            .add(BlockConfig.of("strict", STRICT))
            .build();

    public DataComparatorBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(OPERATION, Operation.EQUAL).setValue(STRICT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPERATION);
        builder.add(STRICT);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        if (state.getValue(FACING_OUTPUT) == direction.getOpposite() && world.getBlockEntity(pos) instanceof DoubleInputTransformerBlockEntity be) {
            return be.lastOutput().isTrue() ? 15 : 0;
        }
        return 0;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        if (state.is(oldState.getBlock()) && (state.getValue(OPERATION) != oldState.getValue(OPERATION))
                && world instanceof ServerLevel serverWorld && world.getBlockEntity(pos) instanceof DoubleInputTransformerBlockEntity be) {
            sendData(world, state.getValue(FACING_OUTPUT), pos, this.transformData(be.lastInput1(), be.lastInput2(), serverWorld, pos, state, be));
        } else {
            world.updateNeighborsAt(pos, this, null);
        }
    }

    @Override
    public int sendData(LevelAccessor world, Direction direction, BlockPos selfPos, DataContainer data) {
        var i = super.sendData(world, direction, selfPos, data);
        if (world instanceof Level w) {
            w.updateNeighborsAt(selfPos, this, null);
        }
        return i;
    }

    @Override
    protected DataContainer transformData(DataContainer input1, DataContainer input2, ServerLevel world, BlockPos selfPos, BlockState selfState, DoubleInputTransformerBlockEntity be) {
        var action = selfState.getValue(OPERATION);
        var strict = selfState.getValue(STRICT);

        if (FactoryUtil.getClosestPlayer(world, selfPos, 16) instanceof ServerPlayer player) {
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

    public enum Operation implements StringRepresentable {
        EQUAL("equal", i -> i == 0),
        NOT_EQUAL("not_equal", i -> i != 0),
        LESS_THAN("less_than", i -> i < 0),
        LESS_OR_EQUAL("less_or_equal", i -> i <= 0),
        MORE_THAN("more_than", i -> i > 0),
        MORE_OR_EQUAL("more_or_equal", i -> i >= 0)
        ;

        private final String name;
        private final IntPredicate predicate;
        public final Component text;

        Operation(String name, IntPredicate predicate) {
            this.name = name;
            this.predicate = predicate;
            this.text = Component.translatable("item.polyfactory.wrench.action.operation.comparator." + name);
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return WRENCH_ACTIONS;
    }
}
