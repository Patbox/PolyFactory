package eu.pb4.polyfactory.block.mechanical.conveyor;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.mojang.serialization.Codec;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.FastItemDisplayElement;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.item.FactoryEnchantmentEffectComponents;
import eu.pb4.polyfactory.item.FactoryEnchantments;
import eu.pb4.polyfactory.models.ConveyorModels;
import eu.pb4.polyfactory.nodes.mechanical.ConveyorNode;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.MovingItemContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItem;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ConveyorBlock extends RotationalNetworkBlock implements FactoryBlock, EntityBlock, ConveyorLikeDirectional, MovingItemConsumer {
    public static final EnumProperty<Direction> DIRECTION = FactoryProperties.HORIZONTAL_DIRECTION;
    public static final EnumProperty<DirectionValue> VERTICAL = EnumProperty.create("vertical", DirectionValue.class);


    public static final BooleanProperty TOP_CONVEYOR = BooleanProperty.create("hide_top");
    public static final BooleanProperty BOTTOM_CONVEYOR = BooleanProperty.create("hide_bottom");
    public static final BooleanProperty PREVIOUS_CONVEYOR = BooleanProperty.create("hide_back");
    public static final BooleanProperty NEXT_CONVEYOR = BooleanProperty.create("hide_front");
    public static final BooleanProperty HAS_OUTPUT_TOP = BooleanProperty.create("has_top_output");

    public ConveyorBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(VERTICAL, DirectionValue.NONE).setValue(DIRECTION, Direction.NORTH).setValue(HAS_OUTPUT_TOP, false)
                .setValue(NEXT_CONVEYOR, false).setValue(PREVIOUS_CONVEYOR, false).setValue(TOP_CONVEYOR, false).setValue(BOTTOM_CONVEYOR, false));
    }

    public static int getModelId(BlockState state) {
        var value = state.getValue(VERTICAL);
        if (value.stack && value.value == 1) {
            return getModelId(state.getValue(PREVIOUS_CONVEYOR), state.getValue(NEXT_CONVEYOR), state.getValue(BOTTOM_CONVEYOR), state.getValue(TOP_CONVEYOR));
        } else if (value.stack && value.value == -1) {
            return getModelId(state.getValue(NEXT_CONVEYOR), state.getValue(PREVIOUS_CONVEYOR), state.getValue(TOP_CONVEYOR), state.getValue(BOTTOM_CONVEYOR));
        }

        return getModelId(state.getValue(TOP_CONVEYOR), state.getValue(BOTTOM_CONVEYOR), state.getValue(PREVIOUS_CONVEYOR), state.getValue(NEXT_CONVEYOR));
    }

    public static int getModelId(boolean top, boolean bottom, boolean previous, boolean next) {
        int i = 0;
        if (top) {
            i |= 1;
        }
        if (bottom) {
            i |= 2;
        }
        if (previous) {
            i |= 4;
        }
        if (next) {
            i |= 8;
        }
        return i;
    }

    public static boolean hasTop(int i) {
        return (i & 1) != 0;
    }

    public static boolean hasBottom(int i) {
        return (i & 2) != 0;
    }

    public static boolean hasPrevious(int i) {
        return (i & 4) != 0;
    }

    public static boolean hasNext(int i) {
        return (i & 8) != 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DIRECTION);
        builder.add(VERTICAL);
        builder.add(HAS_OUTPUT_TOP);
        builder.add(NEXT_CONVEYOR);
        builder.add(PREVIOUS_CONVEYOR);
        builder.add(TOP_CONVEYOR);
        builder.add(BOTTOM_CONVEYOR);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        var x = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (x.is(Items.SLIME_BALL) && this == FactoryBlocks.CONVEYOR) {
            var delta = 0d;
            MovingItem itemContainer = null;

            if (world.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
                itemContainer = conveyor.pullAndRemove();
                delta = conveyor.delta;
            }

            world.setBlockAndUpdate(pos, FactoryBlocks.STICKY_CONVEYOR.withPropertiesOf(state));
            world.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(Blocks.SLIME_BLOCK.defaultBlockState()));
            if (!player.isCreative()) {
                x.shrink(1);
            }

            if (itemContainer != null && world.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
                conveyor.pushAndAttach(itemContainer);
                conveyor.setDelta(delta);
            }

            return InteractionResult.SUCCESS_SERVER;
        } else if (x.is(Items.WET_SPONGE) && this == FactoryBlocks.STICKY_CONVEYOR) {
            var delta = 0d;
            MovingItem itemContainer = null;

            if (world.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
                itemContainer = conveyor.pullAndRemove();
                delta = conveyor.delta;
            }

            world.setBlockAndUpdate(pos, FactoryBlocks.CONVEYOR.withPropertiesOf(state));
            world.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(Blocks.SLIME_BLOCK.defaultBlockState()));
            if (!player.isCreative()) {
                player.getInventory().placeItemBackInInventory(Items.SLIME_BALL.getDefaultInstance());
            }

            if (itemContainer != null && world.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
                conveyor.pushAndAttach(itemContainer);
                conveyor.setDelta(delta);
            }

            return InteractionResult.SUCCESS_SERVER;
        }


        if (x.isEmpty()) {
            var be = world.getBlockEntity(pos);
            if (be instanceof ConveyorBlockEntity conveyor && !conveyor.getItem(0).isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, conveyor.getItem(0));
                conveyor.setItem(0, ItemStack.EMPTY);
                conveyor.setDelta(0);
                return InteractionResult.SUCCESS_SERVER;
            }
        }


        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        state = super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (direction.getAxis() == Direction.Axis.Y) {
            var vert = state.getValue(VERTICAL);
            if (vert.value != 0 && direction == Direction.UP) {
                if (neighborState.is(this)) {
                    state = state.setValue(VERTICAL, switch (vert) {
                        case POSITIVE -> DirectionValue.POSITIVE_STACK;
                        case NEGATIVE -> DirectionValue.NEGATIVE_STACK;
                        default -> vert;
                    });
                } else {
                    state = state.setValue(VERTICAL, switch (vert) {
                        case POSITIVE_STACK -> DirectionValue.POSITIVE;
                        case NEGATIVE_STACK -> DirectionValue.NEGATIVE;
                        default -> vert;
                    });
                }
            }

            return state.setValue(HAS_OUTPUT_TOP, world.getBlockState(pos.above()).is(FactoryBlockTags.CONVEYOR_TOP_OUTPUT)).setValue(direction == Direction.UP ? TOP_CONVEYOR : BOTTOM_CONVEYOR, isMatchingConveyor(neighborState, direction, state.getValue(DIRECTION), state.getValue(VERTICAL)));
        } else if (direction.getAxis() == state.getValue(DIRECTION).getAxis()) {
            return state.setValue(direction == state.getValue(DIRECTION) ? NEXT_CONVEYOR : PREVIOUS_CONVEYOR, isMatchingConveyor(neighborState, direction, state.getValue(DIRECTION), state.getValue(VERTICAL)));
        }
        return state;
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (world instanceof ServerLevel serverWorld) {
            pushEntity(serverWorld, state, pos, entity);
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        var be = world.getBlockEntity(pos);
        return be.getClass() == ConveyorBlockEntity.class && !((ConveyorBlockEntity) be).getItem(0).isEmpty() ? 15 : 0;
    }

    private void pushEntity(ServerLevel world, BlockState state, BlockPos pos, Entity entity) {
        if (entity instanceof ItemEntity itemEntity && itemEntity.tickCount > 0) {
            var be = world.getBlockEntity(pos);
            if (be instanceof ConveyorBlockEntity conveyorBlockEntity && conveyorBlockEntity.tryAdding(itemEntity.getItem())) {
                conveyorBlockEntity.setDelta(0.5);
                if (itemEntity.getItem().isEmpty()) {
                    entity.discard();
                }
            }
            return;
        }

        float mult = 1;

        if (entity instanceof LivingEntity livingEntity) {
            mult = FactoryEnchantments.getMultiplier(livingEntity, FactoryEnchantmentEffectComponents.CONVEYOR_PUSH_MULTIPLIER);
            if (mult <= 0) {
                return;
            }
        }

        var dir = state.getValue(DIRECTION);

        var next = entity.blockPosition().relative(dir);

        var speed = RotationUser.getRotation(world, pos).speed() * Mth.DEG_TO_RAD * 0.9 * 0.7 * mult;

        if (speed == 0) {
            return;
        }
        var vert = state.getValue(VERTICAL);
        if (vert != ConveyorBlock.DirectionValue.NONE) {
            speed = speed / Mth.SQRT_OF_TWO;
        }
        var vec = Vec3.atLowerCornerOf(dir.getUnitVec3i()).scale(speed);
        if (entity.maxUpStep() < 0.51) {
            var box = entity.getBoundingBox().move(vec);
            if (vert == DirectionValue.POSITIVE) {
                for (var shape : state.getCollisionShape(world, pos).toAabbs()) {
                    if (shape.move(pos).intersects(box)) {
                        entity.move(MoverType.SELF, new Vec3(0, 0.51, 0));
                        entity.move(MoverType.SELF, FactoryUtil.safeVelocity(vec));
                        return;
                    }
                }
            }

            var nextState = world.getBlockState(next);
            if (nextState.is(this) && (nextState.getValue(VERTICAL) == DirectionValue.POSITIVE)) {
                for (var shape : state.getCollisionShape(world, pos).toAabbs()) {
                    if (shape.move(next).intersects(box)) {
                        entity.move(MoverType.SELF, new Vec3(0, 0.51, 0));
                        entity.move(MoverType.SELF, FactoryUtil.safeVelocity(vec));
                        return;
                    }
                }
            }
        }

        FactoryUtil.addSafeVelocity(entity, vec);
        ServerPlayer player;
        //noinspection ConstantValue
        if ((entity instanceof ServerPlayer playerx && (player = playerx) != null) || (entity.getControllingPassenger() instanceof ServerPlayer playerx2 && (player = playerx2) != null)) {
            player.connection.send(
                    new ClientboundTeleportEntityPacket(entity.getId(),
                            new PositionMoveRotation(Vec3.ZERO, vec, 0, 0),
                            Set.of(Relative.X, Relative.Y, Relative.Z, Relative.X_ROT, Relative.Y_ROT, Relative.DELTA_Z, Relative.DELTA_X, Relative.DELTA_Y),
                            entity.onGround()
                    )
            );
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        var against = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(ctx.getClickedFace().getOpposite()));
        BlockState state = null;
        Direction direction = Direction.NORTH;
        if (against.is(this)) {
            if (ctx.getClickedFace().getAxis() == Direction.Axis.Y) {
                var behind = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(against.getValue(DIRECTION).getOpposite()));
                state = this.defaultBlockState().setValue(DIRECTION, against.getValue(DIRECTION)).setValue(VERTICAL, behind.is(this) ? DirectionValue.NEGATIVE : DirectionValue.POSITIVE);
            } else if (ctx.getClickedFace().getAxis() == against.getValue(DIRECTION).getAxis()) {
                state = this.defaultBlockState().setValue(DIRECTION, against.getValue(DIRECTION));
                var y = ctx.getClickLocation().y() - ctx.getClickedPos().getY();
                if (y < 5 / 16f) {
                    state = state.setValue(VERTICAL, ctx.getClickedFace() == against.getValue(DIRECTION) ? DirectionValue.NEGATIVE : DirectionValue.POSITIVE);
                }
            }
            direction = against.getValue(DIRECTION);
        }


        if (state == null) {
            if (ctx.getClickedFace().getAxis() != Direction.Axis.Y) {
                direction = ctx.getClickedFace().getOpposite();
                state = this.defaultBlockState().setValue(DIRECTION, ctx.getClickedFace().getOpposite());
            } else {
                direction = ctx.getHorizontalDirection();
                state = this.defaultBlockState().setValue(DIRECTION, ctx.getHorizontalDirection());
            }
        }

        var upState = ctx.getLevel().getBlockState(ctx.getClickedPos().above());

        if (upState.is(this) && upState.getValue(DIRECTION) == direction) {
            if (upState.getValue(VERTICAL).value != 0) {
                var vert = upState.getValue(VERTICAL);
                state = state.setValue(VERTICAL, switch (vert) {
                    case POSITIVE, POSITIVE_STACK -> DirectionValue.POSITIVE_STACK;
                    case NEGATIVE, NEGATIVE_STACK -> DirectionValue.NEGATIVE_STACK;
                    default -> vert;
                });
            } else if (ctx.getClickedFace() == Direction.DOWN) {
                state = state.setValue(VERTICAL, DirectionValue.NEGATIVE_STACK);
            }
        }

        var value = state.getValue(VERTICAL);

        var bottom = ctx.getLevel().getBlockState(ctx.getClickedPos().below());

        return state
                .setValue(NEXT_CONVEYOR, isMatchingConveyor(ctx.getLevel().getBlockState(ctx.getClickedPos().relative(direction)), direction, direction, value))
                .setValue(PREVIOUS_CONVEYOR, isMatchingConveyor(ctx.getLevel().getBlockState(ctx.getClickedPos().relative(direction, -1)), direction.getOpposite(), direction, value))
                .setValue(TOP_CONVEYOR, isMatchingConveyor(ctx.getLevel().getBlockState(ctx.getClickedPos().above()), Direction.UP, direction, value))
                .setValue(BOTTOM_CONVEYOR, isMatchingConveyor(bottom, Direction.DOWN, direction, value))
                .setValue(HAS_OUTPUT_TOP, ctx.getLevel().getBlockState(ctx.getClickedPos().above()).is(FactoryBlockTags.CONVEYOR_TOP_OUTPUT));
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean isMatchingConveyor(BlockState neighborState, Direction neighborDirection, Direction selfDirection, DirectionValue selfValue) {
        if (!neighborState.is(this) || !(neighborState.getValue(DIRECTION) == selfDirection)) {
            return false;
        }

        var neighborValue = neighborState.getValue(VERTICAL);
        if (neighborValue == DirectionValue.POSITIVE && (neighborDirection == Direction.DOWN || neighborDirection == selfDirection)) {
            return selfValue == DirectionValue.NEGATIVE;
        } else if (neighborValue == DirectionValue.NEGATIVE && (neighborDirection == Direction.DOWN || neighborDirection == selfDirection.getOpposite())) {
            return selfValue == DirectionValue.POSITIVE;
        } else if (selfValue == DirectionValue.POSITIVE && (neighborDirection == Direction.UP || neighborDirection == selfDirection.getOpposite())) {
            return neighborValue == DirectionValue.NEGATIVE;
        } else if (selfValue == DirectionValue.NEGATIVE && (neighborDirection == Direction.UP || neighborDirection == selfDirection)) {
            return neighborValue == DirectionValue.POSITIVE;
        }

        if (neighborDirection == Direction.UP || neighborDirection == Direction.DOWN) {
            return neighborValue.value != -selfValue.value;
        }

        return true;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        if (state.getValue(VERTICAL) == DirectionValue.NONE || state.getValue(VERTICAL).stack) {
            return Blocks.BARRIER.defaultBlockState();
        } else {
            return Blocks.ANDESITE_STAIRS.defaultBlockState().setValue(StairBlock.HALF, Half.BOTTOM).setValue(StairBlock.FACING, state.getValue(VERTICAL).value == 1 ? state.getValue(DIRECTION) : state.getValue(DIRECTION).getOpposite());
        }
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new ConveyorNode(state.getValue(DIRECTION), state.getValue(VERTICAL)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConveyorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel && type == FactoryBlockEntities.CONVEYOR ? ConveyorBlockEntity::tick : null;
    }

    @Override
    public TransferMode getTransferMode(BlockState selfState, Direction direction) {
        var dir = selfState.getValue(DIRECTION);

        if (dir == direction) {
            return TransferMode.TO_CONVEYOR;
        }

        return TransferMode.FROM_CONVEYOR;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
        world.updateNeighbourForOutputSignal(pos, this);
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public Vec3 getElementHolderOffset(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return FactoryUtil.HALF_BELOW;
    }

    @Override
    public boolean pushItemTo(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainerHolder conveyor) {
        var state = self.getBlockState();
        var vert = state.getValue(VERTICAL);
        if (!state.is(FactoryBlocks.STICKY_CONVEYOR) && vert.stack) {
            return vert.value == 1;
        }

        if (self.getBlockEntity() instanceof ConveyorBlockEntity be && be.isContainerEmpty()) {
            var selfDir = be.getBlockState().getValue(DIRECTION);

            if (selfDir == pushDirection) {
                be.setDelta(0);
            } else if (selfDir == pushDirection.getOpposite()) {
                be.setDelta(0.8);
            } else {
                be.setDelta(0.5);
            }

            be.pushAndAttach(conveyor.pullAndRemove());
            return true;
        }

        return true;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, DIRECTION);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FactoryUtil.transform(state, mirror::mirror, DIRECTION);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.SMOOTH_STONE.defaultBlockState();
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    public enum DirectionValue implements StringRepresentable {
        NONE(0, false),
        POSITIVE(1, false),
        POSITIVE_STACK(1, true),
        NEGATIVE(-1, false),
        NEGATIVE_STACK(-1, true);

        public static final Codec<DirectionValue> CODEC = StringRepresentable.fromValues(DirectionValue::values);


        public final int value;
        public final boolean stack;

        DirectionValue(int value, boolean isStack) {
            this.value = value;
            this.stack = isStack;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public final class Model extends BlockModel implements MovingItemContainerHolder {
        private final FastItemDisplayElement base;
        private double speed;
        private Direction direction;
        private MovingItem movingItemContainer;
        private DirectionValue value;
        private double delta;

        private Model(ServerLevel world, BlockState state) {
            var type = state.getValue(ConveyorBlock.VERTICAL);
            this.base = new FastItemDisplayElement(getModelForSpeed(0, type, state.is(FactoryBlocks.STICKY_CONVEYOR), state));
            this.base.setFastItem(getFastModel(type, state.is(FactoryBlocks.STICKY_CONVEYOR), state), 24);
            this.base.setDisplaySize(1, 1);
            this.base.setItemDisplayContext(ItemDisplayContext.FIXED);
            this.base.setViewRange(0.7f);
            this.base.setInvisible(true);

            this.updateAnimation(state.getValue(ConveyorBlock.DIRECTION), state.getValue(ConveyorBlock.VERTICAL));
            this.addElement(this.base);
        }

        private ItemStack getModelForSpeed(double speed, DirectionValue directionValue, boolean sticky, BlockState state) {
            return (switch (directionValue) {
                case POSITIVE -> sticky ? ConveyorModels.ANIMATION_UP_STICKY : ConveyorModels.ANIMATION_UP;
                case NEGATIVE -> sticky ? ConveyorModels.ANIMATION_DOWN_STICKY : ConveyorModels.ANIMATION_DOWN;
                default -> sticky ? ConveyorModels.ANIMATION_REGULAR_STICKY : ConveyorModels.ANIMATION_REGULAR;
            })[getModelId(state)][(int) Math.ceil(Mth.clamp(speed * ConveyorModels.FRAMES * 15, 0, ConveyorModels.FRAMES))];
        }

        private ItemStack getFastModel(DirectionValue directionValue, boolean sticky, BlockState state) {
            return (switch (directionValue) {
                case POSITIVE -> sticky ? ConveyorModels.STICKY_UP_FAST : ConveyorModels.UP_FAST;
                case NEGATIVE -> sticky ? ConveyorModels.STICKY_DOWN_FAST : ConveyorModels.DOWN_FAST;
                default -> sticky ? ConveyorModels.STICKY_REGULAR_FAST : ConveyorModels.REGULAR_FAST;
            });
        }

        private void updateAnimation(Direction dir, DirectionValue value) {
            if (dir != this.direction || value != this.value) {
                var mat = mat();
                mat.identity().translate(0, 0.5f, 0).rotateY((270 - dir.toYRot()) * Mth.DEG_TO_RAD);
                if (value.value == -1 && !value.stack) {
                    mat.rotateY(Mth.PI);
                }
                if (value.value != 0 && value.stack) {
                    mat.rotateZ(Mth.HALF_PI * value.value);
                }
                var f = dir.getAxis().ordinal() * 0.0001f;

                var x = value == DirectionValue.NONE || value.stack;
                var v = x ? 0 : 0.0015f;
                mat.translate(-v, v + f, 0);
                mat.scale(x ? 2.001f : 2.015f);
                this.base.setTransformation(mat);
            }
            this.direction = dir;
            this.value = value;
        }

        @Override
        protected void onTick() {
            if (this.movingItemContainer != null) {
                this.movingItemContainer.tick();
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                var state = this.blockState();
                this.base.setItem(getModelForSpeed(speed, state.getValue(ConveyorBlock.VERTICAL), state.is(FactoryBlocks.STICKY_CONVEYOR), state));
                this.base.setFastItem(getFastModel(state.getValue(ConveyorBlock.VERTICAL), state.is(FactoryBlocks.STICKY_CONVEYOR), state), 24);
                this.updateAnimation(state.getValue(ConveyorBlock.DIRECTION), state.getValue(ConveyorBlock.VERTICAL));
                this.tick();
            }
        }

        public boolean updateSpeed(double speed) {
            if (this.speed != speed) {
                var state = this.blockState();
                this.base.setItem(getModelForSpeed(speed, state.getValue(ConveyorBlock.VERTICAL), state.is(FactoryBlocks.STICKY_CONVEYOR), state));
                this.speed = speed;
                return true;
            }
            return false;
        }

        public void updateDelta(double oldDelta, double newDelta) {
            if (oldDelta == newDelta) {
                return;
            }
            this.delta = newDelta;

            if (movingItemContainer != null) {
                movingItemContainer.setPos(calculatePos(newDelta));
                var base = new Quaternionf().rotateY(this.direction.toYRot() * Mth.DEG_TO_RAD);
                if (this.value.stack) {
                    base.rotateX(Mth.HALF_PI);
                } else if (this.value.value != 0) {
                    base.rotateX((this.direction.getAxis() == Direction.Axis.X ? -1 : 1) * Mth.HALF_PI / 2 * -this.value.value);
                }

                movingItemContainer.setRotation(base.mul(Direction.NORTH.getRotation()));
            }
        }

        private Vec3 calculatePos(double delta) {
            if (this.value.stack) {
                var visualDelta = Mth.clamp(delta, 0, 1);
                Vec3i vec3i = this.direction.getUnitVec3i();

                return new Vec3(
                        (-vec3i.getX() * this.value.value * 0.52),
                        this.value.value == -1 ? 1 - visualDelta : visualDelta,
                        (-vec3i.getZ() * this.value.value * 0.52)
                ).add(this.getPos());

            } else {
                var visualDelta = Mth.clamp(delta - 0.5, -0.5, 0.5);
                Vec3i vec3i = this.direction.getUnitVec3i();

                return new Vec3(
                        (vec3i.getX() * visualDelta),
                        Mth.clamp(this.value.value * visualDelta, -1f, 0f) + 1.05,
                        (vec3i.getZ() * visualDelta)
                ).add(this.getPos());
            }
        }

        @Override
        public MovingItem getContainer() {
            return this.movingItemContainer;
        }

        @Override
        public void setContainer(MovingItem container) {
            if (this.movingItemContainer != null) {
                this.removeElement(this.movingItemContainer);
            }

            this.movingItemContainer = container;
            if (container != null) {
                container.setPos(calculatePos(this.delta));
                container.scale(1);
                updateDelta(-1, this.delta);
                this.addElement(container);
            }
        }

        @Override
        public void clearContainer() {
            if (this.movingItemContainer != null) {
                this.removeElement(this.movingItemContainer);
            }
            this.movingItemContainer = null;
        }

        @Override
        public MovingItem pullAndRemove() {
            var x = this.movingItemContainer;
            this.movingItemContainer = null;
            if (x != null) {
                this.removeElementWithoutUpdates(x);
            }

            return x;
        }

        @Override
        public void pushAndAttach(MovingItem container) {
            if (container == null) {
                clearContainer();
                return;
            }

            if (this.movingItemContainer != null) {
                this.removeElement(this.movingItemContainer);
            }

            this.movingItemContainer = container;
            container.scale(1);
            updateDelta(-1, this.delta);
            this.addElementWithoutUpdates(container);
        }
    }
}
