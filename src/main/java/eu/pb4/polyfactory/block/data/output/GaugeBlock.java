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
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import static eu.pb4.polyfactory.ModInit.id;

public class GaugeBlock extends DataNetworkBlock implements FactoryBlock, ConfigurableBlock, EntityBlock, CableConnectable, DataReceiver, PolymerTexturedBlock, QuickWaterloggable {
    public static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;
    public static final EnumProperty<Style> STYLE = EnumProperty.create("style", Style.class);
    public static final EnumProperty<HandPosition> HAND_POSITION = EnumProperty.create("hand_position", HandPosition.class);
    public static final EnumProperty<ValueModifier> VALUE_MODIFIER = EnumProperty.create("value_modifier", ValueModifier.class);
    private static final BlockConfig<Style> STYLE_CONFIG = BlockConfig.of(STYLE);
    private static final BlockConfig<ValueModifier> VALUE_MODIFIER_CONFIG = BlockConfig.of(VALUE_MODIFIER, BlockValueFormatter.text(ValueModifier::text));
    private static final BlockConfig<HandPosition> HAND_POSITION_CONFIG = BlockConfig.of(HAND_POSITION, BlockValueFormatter.text(HandPosition::text));

    public GaugeBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false).setValue(HAND_POSITION, HandPosition.LEFT).setValue(VALUE_MODIFIER, ValueModifier.ABSOLUTE).setValue(STYLE, Style.DEFAULT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED, ORIENTATION, HAND_POSITION, VALUE_MODIFIER, STYLE);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new ChannelReceiverDirectionNode(state.getValue(ORIENTATION).front().getOpposite(), getChannel(world, pos)));
    }

    @Override
    public boolean receiveData(ServerLevel world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, @Nullable DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir) {
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
        return (state.getValue(WATERLOGGED) ? FactoryUtil.TRAPDOOR_WATERLOGGED : FactoryUtil.TRAPDOOR_REGULAR).get(state.getValue(ORIENTATION).front());
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }


    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var dir = ctx.getClickedFace();
        var dir2 = switch (dir) {
            case DOWN -> ctx.getHorizontalDirection();
            case UP -> ctx.getHorizontalDirection().getOpposite();
            default -> Direction.UP;
        };
        return waterLog(ctx, defaultBlockState().setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(dir, dir2)));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(
                BlockConfig.CHANNEL,
                HAND_POSITION_CONFIG,
                VALUE_MODIFIER_CONFIG,
                STYLE_CONFIG

        );
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        var back = pos.relative(state.getValue(ORIENTATION).front().getOpposite());
        world.updateNeighbourForOutputSignal(back, world.getBlockState(back).getBlock());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChanneledDataBlockEntity(pos, state);
    }

    protected int getChannel(ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ChannelContainer container) {
            return container.channel();
        }
        return 0;
    }

    @Override
    public boolean canCableConnect(LevelReader world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.getValue(ORIENTATION).front().getOpposite() == dir;
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
            this.position = state.getValue(HAND_POSITION).value();
            this.modifier = state.getValue(VALUE_MODIFIER);
            this.updateStatePos(state);
            setRotation(0, true);

            this.addElement(this.base);
            this.addElement(this.hand);
        }

        protected void updateStatePos(BlockState state) {
            this.base.setItem(state.getValue(STYLE).model);
            var orientation = state.getValue(ORIENTATION);
            var dir = orientation.front();
            float p = -90;
            float y = 0;

            if (dir.getAxis() == Direction.Axis.Y) {
                if (dir == Direction.DOWN) {
                    p = 90;
                }
                y = orientation.top().toYRot();
            } else {
                p = 0;
                y = dir.toYRot();
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
            return (Mth.clamp( -this.modifier.apply(progress) - this.position, -0.5f, 0.5f) * 2 * 125 * Mth.DEG_TO_RAD);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
                this.position = this.blockState().getValue(HAND_POSITION).value();
                this.modifier = this.blockState().getValue(VALUE_MODIFIER);
                this.targetAngle = this.calculateAngle(this.progress);
            }
        }

        @Override
        protected void onTick() {
            var newAngle = Mth.lerp(0.3f, this.currentAngle, this.targetAngle);

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

    public enum HandPosition implements StringRepresentable {
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

        public Component text() {
            return Component.translatable("text.polyfactory.direction." + this.name);
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public float value() {
            return this.value;
        }
    }

    public enum Style implements StringRepresentable {
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
        public String getSerializedName() {
            return this.name;
        }
    }
}
