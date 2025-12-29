package eu.pb4.polyfactory.block.data.providers;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.configurable.BlockValueFormatter;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.block.data.ChannelContainer;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.AxisAndFacingNetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.property.ValueModifier;
import eu.pb4.polyfactory.data.CapacityData;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.LongData;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public abstract class RotationMeterBlock extends AxisAndFacingNetworkBlock implements FactoryBlock, CableConnectable, DataProvider,
        NetworkComponent.Data, NetworkComponent.Rotational, EntityBlock {
    public RotationMeterBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void updateNetworkAt(LevelReader world, BlockPos pos) {
        Rotational.updateRotationalAt(world, pos);
        Data.updateDataAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof Rotational || block instanceof Data;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.defaultBlockState();
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new FunctionalAxisNode(getAxis(state)));
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new ChannelProviderDirectionNode(state.getValue(FACING), getChannel(world, pos)));
    }

    protected int getChannel(ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ChannelContainer container) {
            return container.channel();
        }
        return 0;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public boolean canCableConnect(LevelReader world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.getValue(FACING) == dir;
    }

    @Override
    public @Nullable DataContainer provideData(ServerLevel world, BlockPos selfPos, BlockState selfState, int channel, DataProviderNode node) {
        if (world.getBlockEntity(selfPos) instanceof ChanneledDataCache be && be.channel() == channel) {
            return be.getCachedData();
        }
        return null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChanneledDataBlockEntity(pos, state);
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(BlockConfig.CHANNEL, BlockConfig.FACING, FIRST_AXIS_CONFIG);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return this::tick;
    }

    protected abstract  <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t);

    public static final class Speed extends RotationMeterBlock {
        public static final EnumProperty<ValueModifier> VALUE_MODIFIER = EnumProperty.create("value_modifier", ValueModifier.class, ValueModifier.ABSOLUTE, ValueModifier.UNMODIFIED);
        private static final BlockConfig<ValueModifier> VALUE_MODIFIER_CONFIG = BlockConfig.of(VALUE_MODIFIER, BlockValueFormatter.text(ValueModifier::text));

        public Speed(Properties settings) {
            super(settings);
            this.registerDefaultState(this.defaultBlockState().setValue(VALUE_MODIFIER, ValueModifier.ABSOLUTE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(VALUE_MODIFIER);
        }

        @Override
        protected <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
            var rot = RotationUser.getRotation(world, pos);
            DataProvider.sendData(world, pos, new LongData((long) (rot.speed() / 360 * 60 * 20 * state.getValue(VALUE_MODIFIER).apply(rot.isNegative() ? -1 : 1))));
        }

        @Override
        public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
            return List.of(BlockConfig.CHANNEL, BlockConfig.FACING, FIRST_AXIS_CONFIG, VALUE_MODIFIER_CONFIG);
        }
    }

    public static final class Stress extends RotationMeterBlock {
        public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);

        public static final BlockConfig<?> TYPE_CONFIG = BlockConfig.of("type", TYPE);

        public Stress(Properties settings) {
            super(settings);
            this.registerDefaultState(this.defaultBlockState().setValue(TYPE, Type.LEFT));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(TYPE);
        }

        @Override
        protected <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
            var rot = RotationUser.getRotation(world, pos);
            DataProvider.sendData(world, pos, switch (state.getValue(TYPE)) {
                case LEFT -> new CapacityData((long) (rot.stressCapacity() - rot.stressUsage()), (long) rot.stressCapacity());
                case CAPACITY -> new LongData((long) rot.stressCapacity());
                case USED ->  new CapacityData((long) rot.stressUsage(), (long) rot.stressCapacity());
            });
        }

        @Override
        public List<BlockConfig<?>> getBlockConfiguration(ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
            return List.of(BlockConfig.CHANNEL, BlockConfig.FACING, FIRST_AXIS_CONFIG, TYPE_CONFIG);
        }

        public enum Type implements StringRepresentable {
            LEFT,
            CAPACITY,
            USED;

            @Override
            public String getSerializedName() {
                return this.name().toLowerCase(Locale.ROOT);
            }
        }
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement axle;
        private final ItemDisplayElement base;

        public Model(BlockState state) {
            this.axle = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.base = ItemDisplayElementUtil.createSolid(state.getBlock().asItem());
            this.base.setScale(new Vector3f(2));

            updateStatePos(state);
            this.updateAnimation(0, state);
            this.addElement(this.axle);
            this.addElement(this.base);
        }

        private void updateAnimation(float speed, BlockState state) {
            var mat = mat();
            mat.identity();
            switch (getAxis(state)) {
                case X -> mat.rotate(Direction.EAST.getRotation());
                case Z -> mat.rotate(Direction.SOUTH.getRotation());
            }

            mat.rotateY(speed);

            mat.scale(2, 2f, 2);
            this.axle.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getGameTime();

            if (tick % this.getUpdateRate() == 0) {

                var facing = this.blockState();

                this.updateAnimation(this.getRotation(), facing);
                this.axle.startInterpolationIfDirty();
            }
        }

        private void updateStatePos(BlockState state) {
            var mat = mat();
            mat.identity();
            mat.rotate(state.getValue(FIRST_AXIS) ? Mth.HALF_PI : 0, state.getValue(FACING).step());
            mat.rotate(state.getValue(FACING).getRotation());
            mat.scale(2f);
            this.base.setTransformation(mat);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
                updateAnimation(this.getRotation(), this.blockState());
                this.axle.setInterpolationDuration(0);
                this.axle.tick();
                this.axle.setInterpolationDuration(this.getUpdateRate());
            }
        }
    }
}
