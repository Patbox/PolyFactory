package eu.pb4.polyfactory.block.data.output;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.mojang.serialization.Codec;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.configurable.ValueFormatter;
import eu.pb4.polyfactory.block.configurable.WrenchModifyValue;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.block.data.ChannelContainer;
import eu.pb4.polyfactory.block.data.DataReceiver;
import eu.pb4.polyfactory.block.data.util.DataNetworkBlock;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.data.BlockStateData;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.ItemStackData;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.nodes.data.ChannelReceiverDirectionNode;
import eu.pb4.polyfactory.nodes.data.DataReceiverNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.DisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static eu.pb4.polyfactory.ModInit.id;

public class HologramProjectorBlock extends DataNetworkBlock implements FactoryBlock, ConfigurableBlock, BlockEntityProvider, CableConnectable, DataReceiver, BarrierBasedWaterloggable {
    public static final BooleanProperty ACTIVE = FactoryProperties.ACTIVE;
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final IntProperty FRONT = FactoryProperties.FRONT;
    private static final BlockConfig<?> CHANGE_ROTATION = BlockConfig.of("front", FRONT, (value, world, pos, side, state) -> {
        var axis = state.get(FACING).getAxis();
        if (axis == Direction.Axis.Y) {
            var rot = Direction.NORTH;
            for (var i = 0; i < value; i++) {
                rot = rot.rotateClockwise(axis);
            }
            return FactoryUtil.asText(rot);
        } else {
            var rot = Direction.UP;
            for (var i = 0; i < value; i++) {
                rot = rot.rotateCounterclockwise(axis);
            }
            return FactoryUtil.asText(rot);
        }
    });

    public static final BlockConfig<?> SCALE = BlockConfig.ofBlockEntity("scale", Codec.FLOAT, HologramProjectorBlockEntity.class,
            ValueFormatter.str(x -> String.format(Locale.ROOT, "%.1f", x)),
            HologramProjectorBlockEntity::scale, HologramProjectorBlockEntity::setScale,
            WrenchModifyValue.simple((x, n) -> FactoryUtil.wrap(x + (n ? 0.5f : -0.5f), 1, 5))
    );

    public static final BlockConfig<?> OFFSET = BlockConfig.ofBlockEntity("offset", Codec.FLOAT, HologramProjectorBlockEntity.class,
            ValueFormatter.str(x -> String.format(Locale.ROOT,"%.1f", x)),
            HologramProjectorBlockEntity::offset, HologramProjectorBlockEntity::setOffset,
            WrenchModifyValue.simple((x, n) -> FactoryUtil.wrap(x + (n ? 0.1f : -0.1f), 0.1f, 1.5f))
    );

    public static final BlockConfig<?> CHANGE_PITCH = BlockConfig.ofBlockEntity("pitch", Codec.FLOAT, HologramProjectorBlockEntity.class,
            ValueFormatter.str(x -> Math.round(x * MathHelper.DEGREES_PER_RADIAN) + "°"),
            HologramProjectorBlockEntity::pitch, HologramProjectorBlockEntity::setPitch,
            WrenchModifyValue.simple((x, n) -> FactoryUtil.wrap(x + MathHelper.RADIANS_PER_DEGREE * (n ? 5 : -5),
                    0, MathHelper.TAU - MathHelper.RADIANS_PER_DEGREE * 5))
    );

    public static final BlockConfig<?> CHANGE_YAW = BlockConfig.ofBlockEntity("yaw", Codec.FLOAT, HologramProjectorBlockEntity.class,
            ValueFormatter.str(x -> Math.round(x * MathHelper.DEGREES_PER_RADIAN) + "°"),
            HologramProjectorBlockEntity::yaw, HologramProjectorBlockEntity::setYaw,
            WrenchModifyValue.simple((x, n) -> FactoryUtil.wrap(x + MathHelper.RADIANS_PER_DEGREE * (n ? 5 : -5),
                    0, MathHelper.TAU - MathHelper.RADIANS_PER_DEGREE * 5))
    );
    public static final BlockConfig<?> CHANGE_ROLL = BlockConfig.ofBlockEntity("roll", Codec.FLOAT, HologramProjectorBlockEntity.class,
            ValueFormatter.str(x -> Math.round(x * MathHelper.DEGREES_PER_RADIAN) + "°"),
            HologramProjectorBlockEntity::roll, HologramProjectorBlockEntity::setRoll,
            WrenchModifyValue.simple((x, n) -> FactoryUtil.wrap(x + MathHelper.RADIANS_PER_DEGREE * (n ? 5 : -5),
                    0, MathHelper.TAU - MathHelper.RADIANS_PER_DEGREE * 5))
    );

    public static final BlockConfig<?> FORCE_TEXT = BlockConfig.ofBlockEntity("force_text", Codec.BOOL, HologramProjectorBlockEntity.class,
            ValueFormatter.text(ScreenTexts::onOrOff),
            HologramProjectorBlockEntity::forceText, HologramProjectorBlockEntity::setForceText,
            WrenchModifyValue.simple((x, n) -> !x)
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
                    ? ctx.getHorizontalPlayerFacing().getHorizontalQuarterTurns() : ctx.getHorizontalPlayerFacing().getOpposite().getHorizontalQuarterTurns();
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
    public boolean canCableConnect(WorldView world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        return state.get(FACING).getOpposite() == dir;
    }

    protected int getChannel(ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ChannelContainer container) {
            return container.channel();
        }
        return 0;
    }

    public List<BlockConfig<?>> getBlockConfiguration(ServerPlayerEntity player, BlockPos blockPos, Direction side, BlockState state) {
        return List.of(
                BlockConfig.CHANNEL_WITH_DISABLED,
                BlockConfig.FACING,
                CHANGE_ROTATION,
                SCALE,
                OFFSET,
                CHANGE_PITCH,
                CHANGE_YAW,
                CHANGE_ROLL,
                FORCE_TEXT
        );
    }

    /*@Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, FACING);
    }*/

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.IRON_BLOCK.getDefaultState();
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        tickWater(state, world, tickView, pos);
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ChannelReceiverDirectionNode(state.get(FACING).getOpposite(), getChannel(world, pos)));
    }

    @Override
    public boolean receiveData(ServerWorld world, BlockPos selfPos, BlockState selfState, int channel, DataContainer data, DataReceiverNode node, BlockPos sourcePos, @Nullable Direction sourceDir) {
        if (world.getBlockEntity(selfPos) instanceof HologramProjectorBlockEntity be) {
            be.setCachedData(data);
            var active = selfState.get(ACTIVE);
            if (data.isEmpty() == active) {
                if (!data.isEmpty() && FactoryUtil.getClosestPlayer(world, selfPos, 32) instanceof ServerPlayerEntity player) {
                    TriggerCriterion.trigger(player, FactoryTriggers.HOLOGRAM_PROJECTOR_ACTIVATES);
                }

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
        private static final ItemStack ACTIVE_MODEL = ItemDisplayElementUtil.getModel(id("block/hologram_projector_active"));
        private static final ParticleEffect EFFECT = new DustColorTransitionParticleEffect(
                ColorHelper.getArgb(0, 153, 250, 255),
                ColorHelper.getArgb(0, 235, 254, 255),
                0.2f);

        private static final Random RANDOM = Random.create();
        private final ItemDisplayElement base;
        private DisplayElement currentDisplay;
        private DisplayElement currentDisplayExtra;
        private Direction facing;
        private boolean active;
        private Direction.Axis axis;
        private float scale;
        private float offset;
        private float pitch;
        private float yaw;
        private float roll;
        private float extraScale = 1;
        private boolean forceText = false;
        private DataContainer lastData;
        private float customCenter;
        private float extraOffset;

        public Model(BlockState state) {
            this.base = ItemDisplayElementUtil.createSimple(state.get(ACTIVE) ? ACTIVE_MODEL : ItemDisplayElementUtil.getModel(state.getBlock().asItem()));
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
                        ColorHelper.getArgb(133, 250, 255),
                        ColorHelper.getArgb(235, 254, 255),
                        0.8f), false, false, pos.x, pos.y, pos.z, 0, 0, 0, 0, 0));

                if (this.currentDisplay != null) {
                    this.currentDisplay.startInterpolationIfDirty();
                }

                if (this.currentDisplayExtra != null) {
                    this.currentDisplayExtra.startInterpolationIfDirty();
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
            float p = -90;
            float y = rot.getPositiveHorizontalDegrees();

            y = rot.getPositiveHorizontalDegrees();
            var q = new Quaternionf().rotateX(MathHelper.HALF_PI);
            if (dir.getAxis() != Direction.Axis.Y) {
                p = 0;
                y = dir.getPositiveHorizontalDegrees();
                q.rotateY((float) (MathHelper.HALF_PI * state.get(FRONT)));
            } else if (dir == Direction.DOWN) {
                p = 90;
            }
            this.base.setLeftRotation(q);
            this.base.setYaw(y);
            this.base.setPitch(p);
            if (this.currentDisplay != null) {
                applyPositionTransformation(this.currentDisplay, this.facing, 0);
            }
            if (this.currentDisplayExtra != null) {
                applyPositionTransformation(this.currentDisplayExtra, this.facing, 1);
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                var state = this.blockState();
                this.base.setItem(state.get(ACTIVE) ? ACTIVE_MODEL : ItemDisplayElementUtil.getModel(state.getBlock().asItem()));
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
            if (data.equals(this.lastData)) {
                return;
            }
            this.lastData = data;

            DisplayElement newDisplay = null;
            DisplayElement newDisplayExtra = null;
            if (data instanceof ItemStackData stackData && !this.forceText) {
                if (this.currentDisplay instanceof ItemDisplayElement e) {
                    e.setItem(stackData.stack());
                    e.tick();
                    return;
                }
                this.extraScale = 1;
                this.customCenter = 0;
                this.extraOffset = 0;
                var i = new ItemDisplayElement(stackData.stack());
                i.setItemDisplayContext(ItemDisplayContext.GUI);
                newDisplay = i;
            } else if (data instanceof BlockStateData blockStateData && !this.forceText) {
                var asItem = (blockStateData.state().getBlock() instanceof FactoryBlock
                || blockStateData.state().getBlock() instanceof SkullBlock)
                        && blockStateData.state().getBlock().asItem() != null;
                if (this.currentDisplay instanceof BlockDisplayElement e && !asItem) {
                    e.setBlockState(blockStateData.state());
                    e.tick();
                    return;
                } else if (this.currentDisplay instanceof ItemDisplayElement e && asItem) {
                    e.setItem(ItemDisplayElementUtil.getModel(blockStateData.state().getBlock().asItem()));
                    e.tick();
                    this.extraScale = 2;
                    return;
                }
                this.extraScale = 1;
                this.customCenter = 0;
                this.extraOffset = 0;
                if (asItem) {
                    var i = new ItemDisplayElement(ItemDisplayElementUtil.getModel(blockStateData.state().getBlock().asItem()));
                    i.setItemDisplayContext(ItemDisplayContext.FIXED);
                    this.extraScale = 2;
                    newDisplay = i;
                } else {
                    newDisplay = new BlockDisplayElement(blockStateData.state());
                }
            } else if (!data.isEmpty()) {
                var list = new ArrayList<String>();
                var string = data.asString();
                var space = -1;
                var start = 0;

                for (var i = 0; i < string.length(); i++) {
                    var chr = string.charAt(i);
                    if (chr == '\n') {
                        list.add(string.substring(start, i));
                        start = i + 1;
                        space = -1;
                        continue;
                    } else if (chr == ' ') {
                        space = i;
                    }

                    if (DefaultFonts.VANILLA.getTextWidth(string.substring(start, i + 1), 8) > 100) {
                        if (space != -1) {
                            list.add(string.substring(start, space));
                            start = space + 1;
                            space = -1;
                            i -= 1;
                        } else {
                            list.add(string.substring(start, i - 1));
                            start = i;
                        }
                    }
                }
                if (start <= string.length() - 1) {
                     list.add(string.substring(start));
                }

                var text = Text.literal(String.join("\n", list)).formatted(Formatting.AQUA);
                if (this.currentDisplay instanceof TextDisplayElement display && this.currentDisplayExtra instanceof TextDisplayElement displayExtra) {
                    display.setText(text);
                    display.tick();
                    displayExtra.setText(text);
                    displayExtra.tick();
                    return;
                }
                this.extraScale = 1;
                var t = new TextDisplayElement(text);
                t.setBackground(0);
                t.setShadow(true);
                t.setLineWidth(120);
                newDisplay = t;
                t = new TextDisplayElement(text);
                t.setBackground(0);
                t.setShadow(true);
                t.setLineWidth(120);
                newDisplayExtra = t;
                this.extraOffset = - 4 / 16f;//list.size() / 16f;
                this.customCenter = 0;//list.size() / 8f;
            }

            if (this.currentDisplay != null) {
                this.removeElement(this.currentDisplay);
            }

            if (this.currentDisplayExtra != null) {
                this.removeElement(this.currentDisplayExtra);
            }

            this.currentDisplay = newDisplay;

            if (newDisplay != null) {
                this.applyInitialTransformation(newDisplay, 0);
                this.addElement(newDisplay);
            }

            this.currentDisplayExtra = newDisplayExtra;

            if (newDisplayExtra != null) {
                this.applyInitialTransformation(newDisplayExtra, 1);
                this.addElement(newDisplayExtra);
            }
        }

        private void applyInitialTransformation(DisplayElement display, int id) {
            display.setDisplaySize(0,0);
            display.setInterpolationDuration(4);
            display.setViewRange(0.8f);
            display.setBrightness(new Brightness(15, 15));
            applyPositionTransformation(display, this.facing, id);
        }

        private void applyDynamicTransformation(DisplayElement display, int id) {
            var mat = mat();
            mat.identity();

            mat.rotate(Direction.get(Direction.AxisDirection.POSITIVE, this.facing.getAxis()).getRotationQuaternion());
            if (display instanceof TextDisplayElement && id == 0) {
                mat.rotateY(MathHelper.PI);
            }
            mat.rotateY((float) (MathHelper.HALF_PI * blockState().get(FRONT)));
            mat.rotateZ((this.facing.getDirection() == Direction.AxisDirection.POSITIVE ? 0 : MathHelper.PI));

            mat.translate(0, this.offset + this.scale / 2 , 0);
            mat.scale(new Vector3f(this.scale, this.scale, display instanceof TextDisplayElement ? 1f : 0.01f).mul(this.extraScale));
            //if (display instanceof TextDisplayElement t) {
            //    mat.translate(0, -t.getText().getString().lines().count() * (6 / 16f), 0);
            //}
            mat.translate(0, this.extraOffset, 0);
            mat.translate(0, this.customCenter, 0);
            mat.rotateXYZ(this.pitch, this.yaw, this.roll);
            if (display instanceof BlockDisplayElement) {
                mat.translate(-1f / 2, -1f / 2, -1f / 2);
            }
            mat.translate(0, -this.customCenter, 0);

            display.setTransformation(mat);
        }

        private void applyPositionTransformation(DisplayElement display, Direction facing, int id) {
            //var vec = new Vector3f(0, 0.5f, 0).rotate(facing.getRotationQuaternion());
            //display.setOffset(new Vec3d(vec));
            //display.setYaw(this.base.getYaw());
            //display.setPitch(this.base.getPitch());


            applyDynamicTransformation(display, id);
        }

        public void setTransform(float scale, float offset, float pitch, float yaw, float roll, boolean forceText) {
            var textChange = forceText != this.forceText;
            this.scale = scale;
            this.offset = offset;
            this.forceText = forceText;
            this.pitch = pitch;
            this.yaw = yaw;
            this.roll = roll;

            if (textChange) {
                if (this.lastData != null) {
                    var oldData = this.lastData;
                    this.lastData = null;
                    this.setData(oldData);
                }
            } else {
                if (this.currentDisplay != null) {
                    applyDynamicTransformation(this.currentDisplay, 0);
                }

                if (this.currentDisplayExtra != null) {
                    applyDynamicTransformation(this.currentDisplayExtra, 1);
                }
            }
        }
    }
}
