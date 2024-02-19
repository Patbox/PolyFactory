package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.DataNetworkBlock;
import eu.pb4.polyfactory.block.data.util.GenericDirectionalDataBlock;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class NixieTubeControllerBlock extends GenericDirectionalDataBlock implements DataReceiver {
    public static final BooleanProperty TOP_CONNECTOR = BooleanProperty.of("top_connector");
    public static final BooleanProperty BOTTOM_CONNECTOR = BooleanProperty.of("bottom_connector");
    public static final BooleanProperty POWERED = Properties.POWERED;

    public static final WrenchAction SCROLL_LOOP = WrenchAction.ofBlockEntity("scroll_loop", NixieTubeControllerBlockEntity.class,
            x -> "" + x.scrollLoop(),
            (x, n) -> x.setScrollLoop(!x.scrollLoop())
    );
    public static final WrenchAction SCROLL_SPEED = WrenchAction.ofBlockEntity("scroll_speed", NixieTubeControllerBlockEntity.class,
            x -> String.format(Locale.ROOT,"%.2f char/sec", x.scrollSpeed() == 0 ? 0 : (20f / x.scrollSpeed())),
            (x, n) -> x.setScrollSpeed(FactoryUtil.wrap(x.scrollSpeed() + (n ? 1 : -1), 0, 80))
    );

    public NixieTubeControllerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(TOP_CONNECTOR, false).with(BOTTOM_CONNECTOR, false).with(POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(TOP_CONNECTOR);
        builder.add(BOTTOM_CONNECTOR);
        builder.add(POWERED);
    }

    @Override
    public List<WrenchAction> getWrenchActions() {
        return List.of(
                WrenchAction.CHANNEL,
                SCROLL_SPEED,
                SCROLL_LOOP,
                WrenchAction.FACING
        );
    }
    @Override
    public boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(POWERED) ? 15 : 0;
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data) {
        if (world.getBlockEntity(selfPos) instanceof NixieTubeControllerBlockEntity be && channel == be.channel()) {
            return be.receiveData(data);
        }
        return false;
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        var be = world.getBlockEntity(pos);
        int channel = 0;
        if (be instanceof NixieTubeControllerBlockEntity blockEntity) {
            channel = blockEntity.channel();
        }
        return List.of(new ChannelReceiverDirectionNode(state.get(FACING).getOpposite(), channel));
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new NixieTubeControllerBlockEntity(pos, state);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return type == FactoryBlockEntities.NIXIE_TUBE_CONTROLLER ? NixieTubeControllerBlockEntity::tick : null;
    }
}
