package eu.pb4.polyfactory.block.data.providers;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.block.data.ChannelContainer;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.AxisAndFacingNetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.LongData;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.data.ChannelProviderDirectionNode;
import eu.pb4.polyfactory.nodes.data.DataProviderNode;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public abstract class RotationMeterBlock extends AxisAndFacingNetworkBlock implements FactoryBlock, CableConnectable, DataProvider,
        NetworkComponent.Data, NetworkComponent.Rotational, BlockEntityProvider {
    public RotationMeterBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void updateNetworkAt(WorldAccess world, BlockPos pos) {
        Rotational.updateRotationalAt(world, pos);
        Data.updateDataAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof Rotational || block instanceof Data;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new FunctionalAxisNode(getAxis(state)));
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ChannelProviderDirectionNode(state.get(FACING), getChannel(world, pos)));
    }

    protected int getChannel(ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ChannelContainer container) {
            return container.channel();
        }
        return 0;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public boolean canCableConnect(WorldAccess world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.get(FACING) == dir;
    }

    @Override
    public @Nullable DataContainer provideData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataProviderNode node) {
        if (world.getBlockEntity(selfPos) instanceof ChanneledDataCache be && be.channel() == channel) {
            return be.getCachedData();
        }
        return null;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ChanneledDataBlockEntity(pos, state);
    }

    @Override
    public List<WrenchAction> getWrenchActions(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(WrenchAction.CHANNEL, WrenchAction.FACING, FIRST_AXIS_ACTION);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return this::tick;
    }

    protected abstract  <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t);

    public static final class Speed extends RotationMeterBlock {
        public Speed(Settings settings) {
            super(settings);
        }

        @Override
        protected <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
            DataProvider.sendData(world, pos, new LongData((long) (RotationUser.getRotation(world, pos).speed() / 360 * 60 * 20)));
        }
    }

    public static final class Stress extends RotationMeterBlock {
        public static final EnumProperty<Type> TYPE = EnumProperty.of("type", Type.class);

        public static final WrenchAction TYPE_ACTION = WrenchAction.of("type", TYPE);

        public Stress(Settings settings) {
            super(settings);
            this.setDefaultState(this.getDefaultState().with(TYPE, Type.LEFT));
        }

        @Override
        protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
            super.appendProperties(builder);
            builder.add(TYPE);
        }

        @Override
        protected <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
            var rot = RotationUser.getRotation(world, pos);
            DataProvider.sendData(world, pos, new LongData(switch (state.get(TYPE)) {
                case LEFT -> (long) (rot.stressCapacity() - rot.stressUsage());
                case CAPACITY -> (long) rot.stressCapacity();
                case USED -> (long) rot.stressUsage();
            }));
        }

        @Override
        public List<WrenchAction> getWrenchActions(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
            return List.of(WrenchAction.CHANNEL, WrenchAction.FACING, FIRST_AXIS_ACTION, TYPE_ACTION);
        }

        public enum Type implements StringIdentifiable {
            LEFT,
            CAPACITY,
            USED;

            @Override
            public String asString() {
                return this.name().toLowerCase(Locale.ROOT);
            }
        }
    }

    public static final class Model extends RotationAwareModel {
        private final ItemDisplayElement axle;
        private final ItemDisplayElement base;

        public Model(BlockState state) {
            this.axle = LodItemDisplayElement.createSimple(AxleBlock.Model.ITEM_MODEL, this.getUpdateRate(), 0.3f, 0.6f);
            this.base = ItemDisplayElementUtil.createSimple(state.getBlock().asItem());
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
                case X -> mat.rotate(Direction.EAST.getRotationQuaternion());
                case Z -> mat.rotate(Direction.SOUTH.getRotationQuaternion());
            }

            mat.rotateY(speed);

            mat.scale(2, 2f, 2);
            this.axle.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();

            if (tick % this.getUpdateRate() == 0) {

                var facing = this.blockState();

                this.updateAnimation(this.getRotation(), facing);
                if (this.axle.isDirty()) {
                    this.axle.startInterpolation();
                }
            }
        }

        private void updateStatePos(BlockState state) {
            var mat = mat();
            mat.identity();
            mat.rotate(state.get(FIRST_AXIS) ? MathHelper.HALF_PI : 0, state.get(FACING).getUnitVector());
            mat.rotate(state.get(FACING).getRotationQuaternion());
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
