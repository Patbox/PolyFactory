package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.QuickWaterloggable;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.BlockValueFormatter;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.block.data.ChannelContainer;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.block.data.util.DataCache;
import eu.pb4.polyfactory.block.data.util.DataNetworkBlock;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.block.property.ValueModifier;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.Orientation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;

import static eu.pb4.polyfactory.ModInit.id;

public class GaugeBlock extends DataNetworkBlock implements FactoryBlock, ConfigurableBlock, BlockEntityProvider, CableConnectable, DataReceiver, PolymerTexturedBlock, QuickWaterloggable {
    public static final EnumProperty<Orientation> ORIENTATION = Properties.ORIENTATION;
    public static final EnumProperty<Style> STYLE = EnumProperty.of("style", Style.class);
    public static final EnumProperty<HandPosition> HAND_POSITION = EnumProperty.of("hand_position", HandPosition.class);
    public static final EnumProperty<ValueModifier> VALUE_MODIFIER = EnumProperty.of("value_modifier", ValueModifier.class);
    private static final BlockConfig<Style> STYLE_CONFIG = BlockConfig.of(STYLE);
    private static final BlockConfig<ValueModifier> VALUE_MODIFIER_CONFIG = BlockConfig.of(VALUE_MODIFIER, BlockValueFormatter.text(ValueModifier::text));
    private static final BlockConfig<HandPosition> HAND_POSITION_CONFIG = BlockConfig.of(HAND_POSITION, BlockValueFormatter.text(HandPosition::text));

    public GaugeBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false).with(HAND_POSITION, HandPosition.LEFT).with(VALUE_MODIFIER, ValueModifier.ABSOLUTE).with(STYLE, Style.DEFAULT));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(WATERLOGGED, ORIENTATION, HAND_POSITION, VALUE_MODIFIER, STYLE);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ChannelReceiverDirectionNode(state.get(ORIENTATION).getFacing().getOpposite(), getChannel(world, pos)));
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, @Nullable DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir) {
        if (world.getBlockEntity(selfPos) instanceof DataCache cache) {
            cache.setCachedData(data);
        }

        final var x = BlockAwareAttachment.get(world, selfPos);
        if (x == null || !(x.holder() instanceof Model model)) {
            return false;
        }

        model.setRotation(data.asProgress(), false);

        return true;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return (state.get(WATERLOGGED) ? FactoryUtil.TRAPDOOR_WATERLOGGED : FactoryUtil.TRAPDOOR_REGULAR).get(state.get(ORIENTATION).getFacing());
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }


    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        var dir = ctx.getSide();
        var dir2 = switch (dir) {
            case DOWN -> ctx.getHorizontalPlayerFacing();
            case UP -> ctx.getHorizontalPlayerFacing().getOpposite();
            default -> Direction.UP;
        };
        return waterLog(ctx, getDefaultState().with(ORIENTATION, Orientation.byDirections(dir, dir2)));
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        tickWater(state, world, tickView, pos);
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(
                BlockConfig.CHANNEL,
                HAND_POSITION_CONFIG,
                VALUE_MODIFIER_CONFIG,
                STYLE_CONFIG

        );
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        var back = pos.offset(state.get(ORIENTATION).getFacing().getOpposite());
        world.updateComparators(back, world.getBlockState(back).getBlock());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ChanneledDataBlockEntity(pos, state);
    }

    protected int getChannel(ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ChannelContainer container) {
            return container.channel();
        }
        return 0;
    }

    @Override
    public boolean canCableConnect(WorldView world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.get(ORIENTATION).getFacing().getOpposite() == dir;
    }

    public static class Model extends BlockModel implements ChanneledDataBlockEntity.InitDataListener {
        private static final ItemStack HAND = ItemDisplayElementUtil.getModel(id("block/gauge_hand"));
        private final ItemDisplayElement base;
        private final ItemDisplayElement hand = ItemDisplayElementUtil.createSimple(HAND);
        private ValueModifier modifier;

        private float targetAngle = 0;
        private float currentAngle = 0;
        private float position;
        private float progress;

        protected Model(BlockState state) {
            super();
            this.base = ItemDisplayElementUtil.createSimple();
            this.base.setScale(new Vector3f(2));
            this.base.setTranslation(new Vector3f(0, 0,-7 / 16f));
            this.hand.setTranslation(this.base.getTranslation());
            this.hand.setInterpolationDuration(1);
            this.hand.setItem(HAND);
            this.position = state.get(HAND_POSITION).value();
            this.modifier = state.get(VALUE_MODIFIER);
            this.updateStatePos(state);
            setRotation(0, true);

            this.addElement(this.base);
            this.addElement(this.hand);
        }

        protected void updateStatePos(BlockState state) {
            this.base.setItem(state.get(STYLE).model);
            var orientation = state.get(ORIENTATION);
            var dir = orientation.getFacing();
            float p = -90;
            float y = 0;

            if (dir.getAxis() == Direction.Axis.Y) {
                if (dir == Direction.DOWN) {
                    p = 90;
                }
                y = orientation.getRotation().getPositiveHorizontalDegrees();
            } else {
                p = 0;
                y = dir.getPositiveHorizontalDegrees();
            }

            this.base.setYaw(y);
            this.base.setPitch(p);
            this.hand.setPitch(p);
            this.hand.setYaw(y);
        }

        public void setRotation(float progress, boolean force) {
            this.progress = progress;
            this.targetAngle = this.calculateAngle(progress);
            if (force) {
                this.currentAngle = this.targetAngle;
                this.hand.setLeftRotation(new Quaternionf().rotateZ(this.targetAngle));
            }
        }

        private float calculateAngle(float progress) {
            return (MathHelper.clamp( -this.modifier.apply(progress) - this.position, -0.5f, 0.5f) * 2 * 125 * MathHelper.RADIANS_PER_DEGREE);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
                this.position = this.blockState().get(HAND_POSITION).value();
                this.modifier = this.blockState().get(VALUE_MODIFIER);
                this.targetAngle = this.calculateAngle(this.progress);
            }
        }

        @Override
        protected void onTick() {
            var newAngle = MathHelper.lerp(0.3f, this.currentAngle, this.targetAngle);

            if (Math.abs(this.currentAngle - newAngle) < 0.001) {
                return;
            }

            this.currentAngle = newAngle;
            this.hand.setLeftRotation(new Quaternionf().rotateZ(newAngle));
            this.hand.startInterpolationIfDirty();
        }

        @Override
        public void provideInitialCachedData(DataContainer lastData) {
            this.setRotation(lastData.asProgress(), true);
        }
    }

    public enum HandPosition implements StringIdentifiable {
        LEFT("left", -0.5f),
        CENTER("center", 0f),
        RIGHT("right", 0.5f),
        ;

        private final String name;
        private final float value;

        HandPosition(String name, float value) {
            this.name = name;
            this.value = value;
        }

        public Text text() {
            return Text.translatable("text.polyfactory.direction." + this.name);
        }

        @Override
        public String asString() {
            return this.name;
        }

        public float value() {
            return this.value;
        }
    }

    public enum Style implements StringIdentifiable {
        DEFAULT("default"),
        RED_GREEN("red_green"),
        GREEN_RED("green_red"),
        ;

        private final String name;
        private final ItemStack model;

        Style(String name) {
            this.name = name;
            this.model = ItemDisplayElementUtil.getModel(id("block/gauge" + (name.equals("default") ? "" : "_" + name)));
        }

        @Override
        public String asString() {
            return this.name;
        }
    }
}
