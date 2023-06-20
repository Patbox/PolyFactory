package eu.pb4.polyfactory.block.machines;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.RotationalNetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.display.LodElementHolder;
import eu.pb4.polyfactory.display.LodItemDisplayElement;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.nodes.mechanical.AxisRotationUserNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polyfactory.util.movingitem.MovingItemProvider;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class PressBlock extends RotationalNetworkBlock implements PolymerBlock, BlockEntityProvider, InventoryProvider, BlockWithElementHolder, RotationUser, MovingItemConsumer, MovingItemProvider, VirtualDestroyStage.Marker {
    public static final Property<Part> PART = EnumProperty.of("part", Part.class);
    public static final BooleanProperty HAS_CONVEYOR = BooleanProperty.of("has_conveyor");
    public static final Property<Direction> INPUT_FACING = DirectionProperty.of("input_facing", x -> x.getAxis() != Direction.Axis.Y);

    public PressBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(PART, Part.MAIN).with(HAS_CONVEYOR, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PART, INPUT_FACING, HAS_CONVEYOR);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return state.get(PART) == Part.TOP ? List.of(new AxisRotationUserNode(state.get(INPUT_FACING).rotateYClockwise().getAxis())) : List.of();
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.ANVIL.getDefaultState();
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        if (
                !ctx.getWorld().getBlockState(ctx.getBlockPos().up()).canReplace(ItemPlacementContext.offset(ctx, ctx.getBlockPos().up(), Direction.DOWN))
        ) {
            return null;
        }

        return this.getDefaultState().with(INPUT_FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(HAS_CONVEYOR, ctx.getWorld().getBlockState(ctx.getBlockPos().offset(ctx.getHorizontalPlayerFacing())).isOf(FactoryBlocks.CONVEYOR));
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (!world.isClient) {
            BlockPos blockPos = pos.up();
            world.setBlockState(blockPos, state.with(PART, Part.TOP), 3);
            world.updateNeighbors(pos, Blocks.AIR);
            state.updateNeighbors(world, pos, 3);
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        var part = state.get(PART);

        if (((part == Part.MAIN && direction == Direction.UP)
                || (part == Part.TOP && direction == Direction.DOWN)
        ) && !neighborState.isOf(this)) {
            NetworkComponent.Rotational.updateRotationalAt(world, pos);
            return Blocks.AIR.getDefaultState();
        } else if (direction == state.get(INPUT_FACING).getOpposite()) {
            return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos).with(HAS_CONVEYOR, neighborState.isOf(FactoryBlocks.CONVEYOR));
        } else {
            return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient && hand == Hand.MAIN_HAND && !player.isSneaking()) {
            pos = state.get(PART) == Part.MAIN ? pos : pos.down();

            if (world.getBlockEntity(pos) instanceof PressBlockEntity be) {
                be.openGui((ServerPlayerEntity) player);
            }
            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (state.get(PART) == Part.MAIN) {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof Inventory) {
                    ItemScatterer.spawn(world, pos, (Inventory) blockEntity);
                    world.updateComparators(pos, this);
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(PART) == Part.MAIN ? new PressBlockEntity(pos, state) : null;
    }

    @Override
    public SidedInventory getInventory(BlockState state, WorldAccess world, BlockPos pos) {
        pos = switch (state.get(PART)) {
            case MAIN -> pos;
            case TOP -> pos.down();
        };

        var be = world.getBlockEntity(pos);

        return be instanceof SidedInventory sidedInventory ? sidedInventory : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.PRESS ? PressBlockEntity::ticker : null;
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return initialBlockState.get(PART) == Part.MAIN ? new Model(world, initialBlockState) : null;
    }

    @Override
    public boolean pushItemTo(BlockPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        if (self.getBlockState().get(INPUT_FACING).getOpposite() != pushDirection || self.getBlockState().get(PART) == Part.TOP) {
            return false;
        }

        var be = (PressBlockEntity) self.getBlockEntity();

        var container = be.getContainerHolder(0);

        if (container.isContainerEmpty()) {
            container.pushAndAttach(conveyor.pullAndRemove());
        } else {
            var targetStack = container.getContainer().get();
            var sourceStack = conveyor.getContainer().get();

            if (ItemStack.canCombine(container.getContainer().get(), conveyor.getContainer().get())) {
                var count = Math.min(targetStack.getCount() + sourceStack.getCount(), container.getMaxStackCount(sourceStack));
                if (count != targetStack.getCount()) {
                    var dec = count - targetStack.getCount();
                    targetStack.increment(dec);
                    sourceStack.decrement(dec);
                }

                if (sourceStack.isEmpty()) {
                    conveyor.clearContainer();
                }
            }
        }

        return true;
    }

    @Override
    public void getItemFrom(BlockPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        var inputDir = self.getBlockState().get(INPUT_FACING);
        if (!conveyor.isContainerEmpty() || pushDirection == inputDir || inputDir.getOpposite() != relative || self.getBlockState().get(PART) == Part.TOP) {
            return;
        }

        var be = (PressBlockEntity) self.getBlockEntity();

        var out = be.getContainerHolder(2);

        if (out.isContainerEmpty()) {
            return;
        }
        var stack = out.getContainer().get();

        var amount = Math.min(stack.getCount(), out.getMaxStackCount(stack));

        if (stack.getCount() == amount) {
            conveyor.pushAndAttach(out.pullAndRemove());
        } else {
            stack.decrement(amount);
            conveyor.setMovementPosition(pushDirection == inputDir.getOpposite() ? 0 : 0.5);
            conveyor.pushNew(stack.copyWithCount(amount));
        }
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof PressBlockEntity be) {
            modifier.stress(be.getStress());
        }
    }

    public final class Model extends LodElementHolder {
        public static final ItemStack MODEL_PISTON = new ItemStack(Items.CANDLE);
        private final Matrix4f mat = new Matrix4f();
        private final ItemDisplayElement piston;
        private final ItemDisplayElement main;
        private float value;

        private Model(ServerWorld world, BlockState state) {
            this.main = new LodItemDisplayElement(FactoryItems.PRESS_BLOCK.getDefaultStack());
            this.main.setDisplaySize(1, 1);
            this.main.setModelTransformation(ModelTransformationMode.FIXED);
            this.piston = new LodItemDisplayElement(MODEL_PISTON);
            this.piston.setDisplaySize(1, 1);
            this.piston.setModelTransformation(ModelTransformationMode.FIXED);
            this.piston.setInterpolationDuration(2);
            this.updateAnimation(state.get(INPUT_FACING));
            this.addElement(this.piston);
            this.addElement(this.main);
        }

        private void updateAnimation(Direction direction) {
            mat.identity().translate(0, 0.469f, 0);
            mat.rotate(direction.getRotationQuaternion().mul(Direction.NORTH.getRotationQuaternion()));
            mat.scale(2f);

            this.main.setTransformation(mat);
            mat.translate(0, 0.5f - this.value * 0.3f, 0);
            this.piston.setTransformation(mat);
        }

        @Override
        protected void onTick() {
            var tick = this.getAttachment().getWorld().getTime();

            if (tick % 2 == 0) {
                this.updateAnimation(BlockBoundAttachment.get(this).getBlockState().get(INPUT_FACING));
                if (this.piston.isDirty()) {
                    this.piston.startInterpolation();
                }
            }
        }

        static {
            MODEL_PISTON.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(MODEL_PISTON.getItem(), FactoryUtil.id("block/press_piston")).value());
        }

        public void updatePiston(double i) {
            if (i < 0) {
                this.value = (float) Math.min(-i * 5f, 1);
            } else {
                this.value = (float) Math.min(i * 1.3, 1);
            }
        }
    }

    public enum Part implements StringIdentifiable {
        MAIN,
        TOP;

        @Override
        public String asString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
