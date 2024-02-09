package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.block.data.ChannelContainer;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.DataNetworkBlock;
import eu.pb4.polyfactory.data.BlockStateData;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.ItemStackData;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.item.wrench.WrenchApplyAction;
import eu.pb4.polyfactory.item.wrench.WrenchValueGetter;
import eu.pb4.polyfactory.item.wrench.WrenchableBlock;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static eu.pb4.polyfactory.ModInit.id;

public class HologramProjectorBlock extends DataNetworkBlock implements FactoryBlock, WrenchableBlock, BlockEntityProvider, CableConnectable, DataReceiver, BarrierBasedWaterloggable {
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
    public static final DirectionProperty FACING = Properties.FACING;
    public static final IntProperty FRONT = IntProperty.of("front", 0, 3);

    private static final WrenchAction CHANGE_ROTATION = WrenchAction.of("front", (world, pos, side, state) -> {
        var axis = state.get(FACING).getAxis();
        if (axis == Direction.Axis.Y) {
            var rot = Direction.NORTH;
            for (var i = 0; i < state.get(FRONT); i++) {
                rot = rot.rotateClockwise(axis);
            }
            return rot.asString();
        } else  {
            var rot = Direction.UP;
            for (var i = 0; i < state.get(FRONT); i++) {
                rot = rot.rotateCounterclockwise(axis);
            }
            return rot.asString();
        }
    }, WrenchApplyAction.ofProperty(FRONT));

    public static final WrenchAction SCALE = WrenchAction.ofBlockEntity("scale", HologramProjectorBlockEntity.class,
            x -> String.format(Locale.ROOT, "%.1f", x.scale()),
            (x, n) -> x.setScale(1 + (4 + (x.scale() - 1) + (n ? 0.5f : -0.5f)) % 4)
    );

    public static final WrenchAction OFFSET = WrenchAction.ofBlockEntity("offset", HologramProjectorBlockEntity.class,
            x -> String.format(Locale.ROOT,"%.1f", x.offset()),
            (x, n) -> x.setOffset((1.5f + x.offset() + (n ? 0.1f : -0.1f)) % 1.5f)
    );

    public static final WrenchAction ROTATION_DISPLAY = WrenchAction.ofBlockEntity("rotation", HologramProjectorBlockEntity.class,
            x -> Math.round(x.rotationDisplay() * MathHelper.DEGREES_PER_RADIAN) + "°",
            (x, n) -> x.setRotationDisplay((MathHelper.TAU + x.rotationDisplay() + MathHelper.RADIANS_PER_DEGREE * (n ? 5 : -5)) % MathHelper.TAU)
    );

    public HologramProjectorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(WATERLOGGED, false).with(ACTIVE, false));
        //noinspection ResultOfMethodCallIgnored
        Model.ACTIVE_MODEL.getCount();
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction direction = ctx.getSide();

        var rot = switch (direction) {
            case UP, DOWN -> ctx.getHorizontalPlayerFacing().getAxis() == Direction.Axis.Z
                    ? ctx.getHorizontalPlayerFacing().getHorizontal() : ctx.getHorizontalPlayerFacing().getOpposite().getHorizontal();
            case NORTH, SOUTH, WEST, EAST -> ctx.getHorizontalPlayerFacing().getAxis() == ctx.getSide().getAxis()
                    ? (ctx.getVerticalPlayerLookDirection() == Direction.UP ? 2 : 0)
                    : ((ctx.getHorizontalPlayerFacing().getDirection() == Direction.AxisDirection.POSITIVE) == (direction.getAxis() == Direction.Axis.Z) ? 1 : 3);
        };

        return waterLog(ctx, this.getDefaultState().with(FACING, direction).with(FRONT, rot));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(WATERLOGGED).add(ACTIVE).add(FACING, FRONT);
    }

    @Override
    public boolean canCableConnect(WorldAccess world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.get(FACING).getOpposite() == dir;
    }

    protected int getChannel(ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ChannelContainer container) {
            return container.channel();
        }
        return 0;
    }

    public List<WrenchAction> getWrenchActions() {
        return List.of(
                WrenchAction.CHANNEL,
                WrenchAction.FACING,
                CHANGE_ROTATION,
                SCALE,
                OFFSET,
                ROTATION_DISPLAY
        );
    }

    /*@Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }*/

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        tickWater(state, world, pos);
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ChannelReceiverDirectionNode(state.get(FACING).getOpposite(), getChannel(world, pos)));
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data) {
        if (world.getBlockEntity(selfPos) instanceof HologramProjectorBlockEntity be) {
            be.setCachedData(data);
            var active = selfState.get(ACTIVE);
            if (data.isEmpty() == active) {
                world.setBlockState(selfPos, selfState.cycle(ACTIVE));
            }
            return true;
        }
        return false;
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
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new HologramProjectorBlockEntity(pos, state);
    }

    public static class Model extends BlockModel {
        private static final ItemStack ACTIVE_MODEL = BaseItemProvider.requestModel(id("block/hologram_projector_active"));
        private static final ParticleEffect EFFECT = new DustColorTransitionParticleEffect(
                new Vector3f(153 / 256f, 250 / 256f, 255 / 256f),
                new Vector3f(235 / 256f, 254 / 256f, 255 / 256f),
                0.2f);

        private static final Random RANDOM = Random.create();
        private final LodItemDisplayElement base;
        private DisplayElement currentDisplay;
        private Direction facing;
        private boolean active;
        private Direction.Axis axis;
        private float scale;
        private float offset;
        private float rotationDisplay;

        public Model(BlockState state) {
            this.base = LodItemDisplayElement.createSimple(state.get(ACTIVE) ? ACTIVE_MODEL : LodItemDisplayElement.getModel(state.getBlock().asItem()));
            this.base.setScale(new Vector3f(2));

            updateStatePos(state);
            this.addElement(this.base);
        }

        @Override
        protected void onTick() {
            if (this.active) {
                var f = scale / 2;
                var x = this.axis.choose(0.1, f, f);
                var y = this.axis.choose(f, 0.1, f);
                var z = this.axis.choose(f, f, 0.1);
                var pos = getPos().offset(this.facing,  this.offset + this.scale / 2)
                        .add(RANDOM.nextFloat() * x * 2 - x, RANDOM.nextFloat() * y * 2 - y, RANDOM.nextFloat() * z * 2 - z);
                this.sendPacket(new ParticleS2CPacket(new DustColorTransitionParticleEffect(
                        new Vector3f(153 / 256f, 250 / 256f, 255 / 256f),
                        new Vector3f(235 / 256f, 254 / 256f, 255 / 256f),
                        0.8f), false, pos.x, pos.y, pos.z, 0, 0, 0, 0, 0));

                if (this.currentDisplay != null) {
                    this.currentDisplay.startInterpolationIfDirty();
                }
            }
        }

        private void updateStatePos(BlockState state) {
            var dir = state.get(FACING);
            var rot = (dir.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP);

            for (var i = 0; i < state.get(FRONT); i++) {
                rot = rot.rotateClockwise(dir.getAxis());
            }
            this.axis = rot.getAxis();
            this.facing = dir;
            this.active = state.get(ACTIVE);
            float p = 0;
            float y = rot.asRotation();

            y = rot.asRotation();
            var q = new Quaternionf();
            if (dir.getAxis() != Direction.Axis.Y) {
                p = 90;
                y = dir.asRotation();
                q.rotateY((float) (MathHelper.HALF_PI * state.get(FRONT)));
            } else if (dir == Direction.DOWN) {
                p = 180;
            }
            this.base.setLeftRotation(q);
            this.base.setYaw(y);
            this.base.setPitch(p);
            if (this.currentDisplay != null) {
                applyPositionTransformation(this.currentDisplay, this.facing);
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                var state = this.blockState();
                this.base.setItem(state.get(ACTIVE) ? ACTIVE_MODEL : LodItemDisplayElement.getModel(state.getBlock().asItem()));
                updateStatePos(state);
                this.base.tick();
                if (this.currentDisplay != null) {
                    var old = this.currentDisplay.getYaw();
                    this.currentDisplay.setYaw(3654);
                    this.currentDisplay.setYaw(old);
                    this.currentDisplay.tick();
                }
            }
        }

        public void setData(DataContainer data) {
            DisplayElement newDisplay;
            if (data instanceof ItemStackData stackData) {
                if (this.currentDisplay instanceof ItemDisplayElement e) {
                    e.setItem(stackData.stack());
                    e.tick();
                    return;
                }
                var i = new ItemDisplayElement(stackData.stack());
                i.setModelTransformation(ModelTransformationMode.GUI);
                newDisplay = i;
            } else if (data instanceof BlockStateData blockStateData) {
                if (this.currentDisplay instanceof BlockDisplayElement e) {
                    e.setBlockState(blockStateData.state());
                    e.tick();
                    return;
                }
                newDisplay = new BlockDisplayElement(blockStateData.state());
            } else if (!data.isEmpty()) {
                var t = new TextDisplayElement(Text.literal(String.join("\n", data.asString().lines().limit(4).toList())));
                t.setBackground(0);
                t.setShadow(true);
                t.setLineWidth(60);
                newDisplay = t;
            } else {
                newDisplay = null;
            }

            if (this.currentDisplay != null) {
                this.removeElement(this.currentDisplay);
            }

            this.currentDisplay = newDisplay;

            if (newDisplay != null) {
                this.applyInitialTransformation(newDisplay);
                this.addElement(newDisplay);
            }
        }

        private void applyInitialTransformation(DisplayElement display) {
            display.setDisplaySize(0,0);
            display.setInterpolationDuration(4);
            display.setViewRange(0.8f);
            display.setBrightness(new Brightness(15, 15));
            applyPositionTransformation(display, this.facing);
        }

        private void applyDynamicTransformation(DisplayElement display) {
            mat.identity();
            if (display instanceof BlockDisplayElement) {
                mat.translate(-this.scale / 2, -this.scale / 2, 0);
            }

            mat.rotate(Direction.get(Direction.AxisDirection.POSITIVE, this.facing.getAxis()).getRotationQuaternion());
            if (display instanceof TextDisplayElement) {
                mat.rotateY(MathHelper.PI);
            }
            mat.rotateY((float) (MathHelper.HALF_PI * blockState().get(FRONT)));

            mat.rotateZ((this.facing.getDirection() == Direction.AxisDirection.POSITIVE ? 0 : MathHelper.PI));
            mat.translate(0, this.offset + this.scale / 2, 0);
            if (display instanceof TextDisplayElement t) {
                mat.translate(0, -t.getText().getString().lines().count() * this.scale * (7 / 16f), 0);
            }
            mat.rotateZ(this.rotationDisplay);

            mat.scale(new Vector3f(this.scale, this.scale, display instanceof TextDisplayElement ? 1 : 0.005f));
            display.setTransformation(mat);
        }

        private void applyPositionTransformation(DisplayElement display, Direction facing) {
            //var vec = new Vector3f(0, 0.5f, 0).rotate(facing.getRotationQuaternion());
            //display.setOffset(new Vec3d(vec));
            //display.setYaw(this.base.getYaw());
            //display.setPitch(this.base.getPitch());


            applyDynamicTransformation(display);
        }

        public void setTransform(float scale, float offset, float rotationDisplay) {
            this.scale = scale;
            this.offset = offset;
            this.rotationDisplay = rotationDisplay;
            if (this.currentDisplay != null) {
                applyDynamicTransformation(this.currentDisplay);
            }
        }
    }
}
