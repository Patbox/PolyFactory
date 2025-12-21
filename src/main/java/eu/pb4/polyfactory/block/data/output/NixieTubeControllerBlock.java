package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.configurable.WrenchModifyBlockValue;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.DirectionalCabledDataBlock;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverSelectiveSideNode;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class NixieTubeControllerBlock extends DirectionalCabledDataBlock implements DataReceiver {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public static final BlockConfig<Boolean> SCROLL_LOOP = BlockConfig.ofBlockEntity("scroll_loop", Codec.BOOL, NixieTubeControllerBlockEntity.class,
            (on, world, pos, side, state) -> CommonComponents.optionStatus(on),
            NixieTubeControllerBlockEntity::scrollLoop, NixieTubeControllerBlockEntity::setScrollLoop,
            WrenchModifyBlockValue.simple((x, n) -> !x)
    );
    public static final BlockConfig<Integer> SCROLL_SPEED = BlockConfig.ofBlockEntity("scroll_speed", Codec.INT, NixieTubeControllerBlockEntity.class,
            (x, world, pos, side, state) -> Component.translatable("text.polyfactory.char_per_sec", String.format(Locale.ROOT,"%.2f", x == 0f ? 0 : (20f / x))),
            NixieTubeControllerBlockEntity::scrollSpeed, NixieTubeControllerBlockEntity::setScrollSpeed,
            WrenchModifyBlockValue.simple((x, n) -> FactoryUtil.wrap(x + (n ? 1 : -1), 0, 80))
    );

    public NixieTubeControllerBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED);
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(
                BlockConfig.CHANNEL_WITH_DISABLED,
                this.facingAction,
                SCROLL_SPEED,
                SCROLL_LOOP
        );
    }
    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction direction) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    public boolean receiveData(ServerLevel world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir) {
        if (world.getBlockEntity(selfPos) instanceof NixieTubeControllerBlockEntity be && channel == be.channel()) {
            return be.receiveData(data);
        }
        return false;
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new ChannelReceiverSelectiveSideNode(getDirections(state), getChannel(world, pos)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NixieTubeControllerBlockEntity(pos, state);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return type == FactoryBlockEntities.NIXIE_TUBE_CONTROLLER ? NixieTubeControllerBlockEntity::tick : null;
    }
}
