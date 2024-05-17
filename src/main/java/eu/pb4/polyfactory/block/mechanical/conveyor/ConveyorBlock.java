package eu.pb4.polyfactory.block.mechanical.conveyor;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.polyfactory.item.FactoryEnchantments;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.models.ConveyorModel;
import eu.pb4.factorytools.api.virtualentity.FastItemDisplayElement;
import eu.pb4.polyfactory.nodes.mechanical.ConveyorNode;
import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItem;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class ConveyorBlock extends RotationalNetworkBlock implements FactoryBlock, BlockEntityProvider, ConveyorLikeDirectional, MovingItemConsumer {
    public static final DirectionProperty DIRECTION = DirectionProperty.of("direction", Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    public static final EnumProperty<DirectionValue> VERTICAL = EnumProperty.of("vertical", DirectionValue.class);


    public static final BooleanProperty TOP_CONVEYOR = BooleanProperty.of("hide_top");
    public static final BooleanProperty BOTTOM_CONVEYOR = BooleanProperty.of("hide_bottom");
    public static final BooleanProperty PREVIOUS_CONVEYOR = BooleanProperty.of("hide_back");
    public static final BooleanProperty NEXT_CONVEYOR = BooleanProperty.of("hide_front");
    public static final BooleanProperty HAS_OUTPUT_TOP = BooleanProperty.of("has_top_output");

    public ConveyorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(VERTICAL, DirectionValue.NONE).with(DIRECTION, Direction.NORTH).with(HAS_OUTPUT_TOP, false)
                .with(NEXT_CONVEYOR, false).with(PREVIOUS_CONVEYOR, false).with(TOP_CONVEYOR, false).with(BOTTOM_CONVEYOR, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(DIRECTION);
        builder.add(VERTICAL);
        builder.add(HAS_OUTPUT_TOP);
        builder.add(NEXT_CONVEYOR);
        builder.add(PREVIOUS_CONVEYOR);
        builder.add(TOP_CONVEYOR);
        builder.add(BOTTOM_CONVEYOR);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        var x = player.getStackInHand(Hand.MAIN_HAND);
        if (x.isOf(Items.SLIME_BALL) && this == FactoryBlocks.CONVEYOR) {
            var delta = 0d;
            MovingItem itemContainer = null;

            if (world.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
                itemContainer = conveyor.pullAndRemove();
                delta = conveyor.delta;
            }

            world.setBlockState(pos, FactoryBlocks.STICKY_CONVEYOR.getStateWithProperties(state));
            world.syncWorldEvent(WorldEvents.BLOCK_BROKEN, pos, Block.getRawIdFromState(Blocks.SLIME_BLOCK.getDefaultState()));
            if (!player.isCreative()) {
                x.decrement(1);
            }

            if (itemContainer != null && world.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
                conveyor.pushAndAttach(itemContainer);
                conveyor.setDelta(delta);
            }

            return ActionResult.SUCCESS;
        } else if (x.isOf(Items.WET_SPONGE) && this == FactoryBlocks.STICKY_CONVEYOR) {
            var delta = 0d;
            MovingItem itemContainer = null;

            if (world.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
                itemContainer = conveyor.pullAndRemove();
                delta = conveyor.delta;
            }

            world.setBlockState(pos, FactoryBlocks.CONVEYOR.getStateWithProperties(state));
            world.syncWorldEvent(WorldEvents.BLOCK_BROKEN, pos, Block.getRawIdFromState(Blocks.SLIME_BLOCK.getDefaultState()));
            if (!player.isCreative()) {
                player.getInventory().offerOrDrop(Items.SLIME_BALL.getDefaultStack());
            }

            if (itemContainer != null && world.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
                conveyor.pushAndAttach(itemContainer);
                conveyor.setDelta(delta);
            }

            return ActionResult.SUCCESS;
        }


        if (x.isEmpty()) {
            var be = world.getBlockEntity(pos);
            if (be instanceof ConveyorBlockEntity conveyor && !conveyor.getStack(0).isEmpty()) {
                player.setStackInHand(Hand.MAIN_HAND, conveyor.getStack(0));
                conveyor.setStack(0, ItemStack.EMPTY);
                conveyor.setDelta(0);
                return ActionResult.SUCCESS;
            }
        }


        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        if (direction.getAxis() == Direction.Axis.Y) {
            var vert = state.get(VERTICAL);
            if (vert.value != 0 && direction == Direction.UP) {
                if (neighborState.isOf(this)) {
                    state = state.with(VERTICAL, switch (vert) {
                        case POSITIVE -> DirectionValue.POSITIVE_STACK;
                        case NEGATIVE -> DirectionValue.NEGATIVE_STACK;
                        default -> vert;
                    });
                } else {
                    state = state.with(VERTICAL, switch (vert) {
                        case POSITIVE_STACK -> DirectionValue.POSITIVE;
                        case NEGATIVE_STACK -> DirectionValue.NEGATIVE;
                        default -> vert;
                    });
                }
            }

            return state.with(HAS_OUTPUT_TOP, world.getBlockState(pos.up()).isIn(FactoryBlockTags.CONVEYOR_TOP_OUTPUT)).with(direction == Direction.UP ? TOP_CONVEYOR : BOTTOM_CONVEYOR, isMatchingConveyor(neighborState, direction, state.get(DIRECTION), state.get(VERTICAL)));
        } else if (direction.getAxis() == state.get(DIRECTION).getAxis()) {
            return state.with(direction == state.get(DIRECTION) ? NEXT_CONVEYOR : PREVIOUS_CONVEYOR, isMatchingConveyor(neighborState, direction, state.get(DIRECTION), state.get(VERTICAL)));
        }
        return state;
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (world instanceof ServerWorld serverWorld) {
            pushEntity(serverWorld, state, pos, entity);
        }
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        var be = world.getBlockEntity(pos);
        return be.getClass() == ConveyorBlockEntity.class && !((ConveyorBlockEntity) be).getStack(0).isEmpty() ? 15 : 0;
    }

    private void pushEntity(ServerWorld world, BlockState state, BlockPos pos, Entity entity) {
        if (entity instanceof ItemEntity itemEntity && itemEntity.age > 0) {
            var be = world.getBlockEntity(pos);
            if (be instanceof ConveyorBlockEntity conveyorBlockEntity && conveyorBlockEntity.tryAdding(itemEntity.getStack())) {
                conveyorBlockEntity.setDelta(0.5);
                if (itemEntity.getStack().isEmpty()) {
                    entity.discard();
                }
            }
            return;
        }

        if (entity instanceof LivingEntity livingEntity
                && EnchantmentHelper.getEquipmentLevel(FactoryEnchantments.IGNORE_MOVEMENT.value(), livingEntity) != 0) {
            return;
        }

        var dir = state.get(DIRECTION);

        var next = entity.getBlockPos().offset(dir);

        var speed = RotationUser.getRotation(world, pos).speed() * MathHelper.RADIANS_PER_DEGREE * 0.9 * 0.7;

        if (speed == 0) {
            return;
        }
        var vert = state.get(VERTICAL);
        if (vert != ConveyorBlock.DirectionValue.NONE) {
            speed = speed / MathHelper.SQUARE_ROOT_OF_TWO;
        }
        var vec = Vec3d.of(dir.getVector()).multiply(speed);
        if (entity.getStepHeight() < 0.51) {
            var box = entity.getBoundingBox().offset(vec);
            if (vert == DirectionValue.POSITIVE) {
                for (var shape : state.getCollisionShape(world, pos).getBoundingBoxes()) {
                    if (shape.offset(pos).intersects(box)) {
                        entity.move(MovementType.SELF, new Vec3d(0, 0.51, 0));
                        entity.move(MovementType.SELF, FactoryUtil.safeVelocity(vec));
                        return;
                    }
                }
            }

            var nextState = world.getBlockState(next);
            if (nextState.isOf(this) && (nextState.get(VERTICAL) == DirectionValue.POSITIVE)) {
                for (var shape : state.getCollisionShape(world, pos).getBoundingBoxes()) {
                    if (shape.offset(next).intersects(box)) {
                        entity.move(MovementType.SELF, new Vec3d(0, 0.51, 0));
                        entity.move(MovementType.SELF, FactoryUtil.safeVelocity(vec));
                        return;
                    }
                }
            }
        }

        FactoryUtil.addSafeVelocity(entity, vec);
        if (entity instanceof ServerPlayerEntity player) {
            FactoryUtil.sendVelocityDelta(player, vec.multiply(0.55));
        }
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var against = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(ctx.getSide().getOpposite()));
        BlockState state = null;
        Direction direction = Direction.NORTH;
        if (against.isOf(this)) {
            if (ctx.getSide().getAxis() == Direction.Axis.Y) {
                var behind = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(against.get(DIRECTION).getOpposite()));
                state = this.getDefaultState().with(DIRECTION, against.get(DIRECTION)).with(VERTICAL, behind.isOf(this) ? DirectionValue.NEGATIVE : DirectionValue.POSITIVE);
            } else if (ctx.getSide().getAxis() == against.get(DIRECTION).getAxis()) {
                state = this.getDefaultState().with(DIRECTION, against.get(DIRECTION));
                var y = ctx.getHitPos().getY() - ctx.getBlockPos().getY();
                if (y < 5 / 16f) {
                    state = state.with(VERTICAL, ctx.getSide() == against.get(DIRECTION) ? DirectionValue.NEGATIVE : DirectionValue.POSITIVE);
                }
            }
            direction = against.get(DIRECTION);
        }


        if (state == null) {
            if (ctx.getSide().getAxis() != Direction.Axis.Y) {
                direction = ctx.getSide().getOpposite();
                state = this.getDefaultState().with(DIRECTION, ctx.getSide().getOpposite());
            } else {
                direction = ctx.getHorizontalPlayerFacing();
                state = this.getDefaultState().with(DIRECTION, ctx.getHorizontalPlayerFacing());
            }
        }

        var upState = ctx.getWorld().getBlockState(ctx.getBlockPos().up());

        if (upState.isOf(this) && upState.get(DIRECTION) == direction) {
            if (upState.get(VERTICAL).value != 0) {
                var vert = upState.get(VERTICAL);
                state = state.with(VERTICAL, switch (vert) {
                    case POSITIVE, POSITIVE_STACK -> DirectionValue.POSITIVE_STACK;
                    case NEGATIVE, NEGATIVE_STACK -> DirectionValue.NEGATIVE_STACK;
                    default -> vert;
                });
            } else if (ctx.getSide() == Direction.DOWN) {
                state = state.with(VERTICAL, DirectionValue.NEGATIVE_STACK);
            }
        }

        var value = state.get(VERTICAL);

        var bottom = ctx.getWorld().getBlockState(ctx.getBlockPos().down());

        return state
                .with(NEXT_CONVEYOR, isMatchingConveyor(ctx.getWorld().getBlockState(ctx.getBlockPos().offset(direction)), direction , direction, value))
                .with(PREVIOUS_CONVEYOR, isMatchingConveyor(ctx.getWorld().getBlockState(ctx.getBlockPos().offset(direction, -1)), direction.getOpposite(), direction, value))
                .with(TOP_CONVEYOR, isMatchingConveyor(ctx.getWorld().getBlockState(ctx.getBlockPos().up()),Direction.UP, direction, value))
                .with(BOTTOM_CONVEYOR, isMatchingConveyor(bottom, Direction.DOWN, direction, value))
                .with(HAS_OUTPUT_TOP, ctx.getWorld().getBlockState( ctx.getBlockPos().up()).isIn(FactoryBlockTags.CONVEYOR_TOP_OUTPUT));
    }

    public static int getModelId(BlockState state) {
        return getModelId(state.get(TOP_CONVEYOR), state.get(BOTTOM_CONVEYOR), state.get(PREVIOUS_CONVEYOR), state.get(NEXT_CONVEYOR));
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

    @SuppressWarnings("RedundantIfStatement")
    private boolean isMatchingConveyor(BlockState neighborState, Direction neighborDirection, Direction selfDirection, DirectionValue selfValue) {
        if (!neighborState.isOf(this) || !(neighborState.get(DIRECTION) == selfDirection)) {
            return false;
        }

        var neighborValue = neighborState.get(VERTICAL);
        if (neighborValue == DirectionValue.POSITIVE && (neighborDirection == Direction.DOWN || neighborDirection == selfDirection)) {
            return selfValue == DirectionValue.NEGATIVE;
        } else if (neighborValue == DirectionValue.NEGATIVE && (neighborDirection == Direction.DOWN || neighborDirection == selfDirection.getOpposite())) {
            return selfValue == DirectionValue.POSITIVE;
        } else if (selfValue == DirectionValue.POSITIVE && (neighborDirection == Direction.UP || neighborDirection == selfDirection.getOpposite())) {
            return neighborValue == DirectionValue.NEGATIVE;
        } else if (selfValue == DirectionValue.NEGATIVE && (neighborDirection == Direction.UP || neighborDirection == selfDirection)) {
            return neighborValue == DirectionValue.POSITIVE;
        }

        return true;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        if (state.get(VERTICAL) == DirectionValue.NONE || state.get(VERTICAL).stack) {
            return Blocks.BARRIER.getDefaultState();
        } else {
            return Blocks.ANDESITE_STAIRS.getDefaultState().with(StairsBlock.HALF, BlockHalf.BOTTOM).with(StairsBlock.FACING, state.get(VERTICAL).value == 1 ? state.get(DIRECTION) : state.get(DIRECTION).getOpposite());
        }
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ConveyorNode(state.get(DIRECTION), state.get(VERTICAL)));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ConveyorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.CONVEYOR ? ConveyorBlockEntity::tick : null;
    }

    @Override
    public TransferMode getTransferMode(BlockState selfState, Direction direction) {
        var dir = selfState.get(DIRECTION);

        if (dir == direction) {
            return TransferMode.TO_CONVEYOR;
        }

        return TransferMode.FROM_CONVEYOR;
    }

    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory) {
                ItemScatterer.spawn(world, pos, (Inventory) blockEntity);
                world.updateComparators(pos, this);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public Vec3d getElementHolderOffset(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return FactoryUtil.HALF_BELOW;
    }

    @Override
    public boolean pushItemTo(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        var state = self.getBlockState();
        var vert = state.get(VERTICAL);
        if (!state.isOf(FactoryBlocks.STICKY_CONVEYOR) && vert.stack) {
            return vert.value == 1;
        }

        if (self.getBlockEntity() instanceof ConveyorBlockEntity be && be.isContainerEmpty()) {
            var selfDir = be.getCachedState().get(DIRECTION);

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
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, DIRECTION);
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return FactoryUtil.transform(state, mirror::apply, DIRECTION);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.SMOOTH_STONE.getDefaultState();
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    public enum DirectionValue implements StringIdentifiable {
        NONE(0, false),
        POSITIVE(1, false),
        POSITIVE_STACK(1, true),
        NEGATIVE(-1, false),
        NEGATIVE_STACK(-1, true);

        public static final Codec<DirectionValue> CODEC = StringIdentifiable.createBasicCodec(DirectionValue::values);


        public final int value;
        public final boolean stack;

        DirectionValue(int value, boolean isStack) {
            this.value = value;
            this.stack = isStack;
        }

        @Override
        public String asString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public final class Model extends BlockModel implements ContainerHolder {
        private final FastItemDisplayElement base;
        private double speed;
        private Direction direction;
        private MovingItem movingItemContainer;
        private DirectionValue value;
        private double delta;

        private Model(ServerWorld world, BlockState state) {
            var type = state.get(ConveyorBlock.VERTICAL);
            this.base = new FastItemDisplayElement(getModelForSpeed(0, type, state.isOf(FactoryBlocks.STICKY_CONVEYOR), state));
            this.base.setFastItem(getFastModel(type, state.isOf(FactoryBlocks.STICKY_CONVEYOR), state), 24);
            this.base.setDisplaySize(1, 1);
            this.base.setModelTransformation(ModelTransformationMode.FIXED);
            this.base.setViewRange(0.7f);
            this.base.setInvisible(true);

            this.updateAnimation(state.get(ConveyorBlock.DIRECTION), state.get(ConveyorBlock.VERTICAL));
            this.addElement(this.base);
        }

        private ItemStack getModelForSpeed(double speed, DirectionValue directionValue, boolean sticky, BlockState state) {
            return (switch (directionValue) {
                case POSITIVE -> sticky ? ConveyorModel.ANIMATION_UP_STICKY : ConveyorModel.ANIMATION_UP;
                case NEGATIVE -> sticky ? ConveyorModel.ANIMATION_DOWN_STICKY : ConveyorModel.ANIMATION_DOWN;
                default -> sticky ? ConveyorModel.ANIMATION_REGULAR_STICKY : ConveyorModel.ANIMATION_REGULAR;
            })[getModelId(state)][(int) Math.ceil(MathHelper.clamp(speed * ConveyorModel.FRAMES * 15, 0, ConveyorModel.FRAMES))];
        }

        private ItemStack getFastModel(DirectionValue directionValue, boolean sticky, BlockState state) {
            return (switch (directionValue) {
                case POSITIVE -> sticky ? ConveyorModel.STICKY_UP_FAST : ConveyorModel.UP_FAST;
                case NEGATIVE -> sticky ? ConveyorModel.STICKY_DOWN_FAST : ConveyorModel.DOWN_FAST;
                default -> sticky ? ConveyorModel.STICKY_REGULAR_FAST : ConveyorModel.REGULAR_FAST;
            });
        }

        private void updateAnimation(Direction dir, DirectionValue value) {
            if (dir != this.direction || value != this.value) {
                mat.identity().translate(0, 0.5f, 0).rotateY((270 - dir.asRotation()) * MathHelper.RADIANS_PER_DEGREE);
                if (value.value == -1 && !value.stack) {
                    mat.rotateY(MathHelper.PI);
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
            if (this.movingItemContainer != null && this.getTick() % 2 == 0) {
                this.movingItemContainer.checkItems();
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                var state = this.blockState();
                this.base.setItem(getModelForSpeed(speed, state.get(ConveyorBlock.VERTICAL), state.isOf(FactoryBlocks.STICKY_CONVEYOR), state));
                this.base.setFastItem(getFastModel(state.get(ConveyorBlock.VERTICAL), state.isOf(FactoryBlocks.STICKY_CONVEYOR), state), 24);
                this.updateAnimation(state.get(ConveyorBlock.DIRECTION), state.get(ConveyorBlock.VERTICAL));
                this.tick();
            }
        }

        public boolean updateSpeed(double speed) {
            if (this.speed != speed) {
                var state = this.blockState();
                this.base.setItem(getModelForSpeed(speed, state.get(ConveyorBlock.VERTICAL), state.isOf(FactoryBlocks.STICKY_CONVEYOR), state));
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
                var base = new Quaternionf().rotateY(this.direction.asRotation() * MathHelper.RADIANS_PER_DEGREE);
                if (this.value.stack) {
                    base.rotateX(MathHelper.HALF_PI);
                } else if (this.value.value != 0) {
                    base.rotateX( (this.direction.getAxis() == Direction.Axis.X ? -1 : 1) * MathHelper.HALF_PI / 2 * -this.value.value);
                }

                movingItemContainer.setRotation(base);
            }
        }

        private Vec3d calculatePos(double delta) {
            if (this.value.stack) {
                var visualDelta = MathHelper.clamp(delta, 0, 1);
                Vec3i vec3i = this.direction.getVector();

                return new Vec3d(
                        (-vec3i.getX() * this.value.value * 0.52),
                        this.value.value == -1 ? 1 - visualDelta : visualDelta,
                        (-vec3i.getZ() * this.value.value * 0.52)
                ).add(this.getPos());

            } else {
                var visualDelta = MathHelper.clamp(delta - 0.5, -0.5, 0.5);
                Vec3i vec3i = this.direction.getVector();

                return new Vec3d(
                        (vec3i.getX() * visualDelta),
                        MathHelper.clamp(this.value.value * visualDelta, -1f, 0f) + 1.05,
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
            updateDelta(-1, this.delta);
            this.addElementWithoutUpdates(container);
        }

        public void tryTick() {
        }
    }
}
