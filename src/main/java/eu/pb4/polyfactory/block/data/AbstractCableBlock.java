package eu.pb4.polyfactory.block.data;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.nodes.generic.SelectiveSideNode;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;

public abstract class AbstractCableBlock extends CableNetworkBlock implements BlockEntityProvider, CableConnectable {
    public static final int DEFAULT_COLOR = 0xbbbbbb;
    public static final BooleanProperty HAS_CABLE = BooleanProperty.of("has_cable");

    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty WEST = Properties.WEST;
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty DOWN = Properties.DOWN;
    public static final Map<Direction, BooleanProperty> FACING_PROPERTIES = FactoryProperties.DIRECTIONS;

    public AbstractCableBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(NORTH, false).with(SOUTH, false)
                .with(EAST, false).with(WEST, false).with(UP, false).with(DOWN, false));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        var hand = Hand.MAIN_HAND;
        if (player.getStackInHand(hand).isIn(ConventionalItemTags.DYES)
                && setColor(state, world, pos, FactoryItems.CABLE.downSampleColor(DyeColorExtra.getColor(player.getStackInHand(hand))))
        ) {
            if (!player.isCreative()) {
                player.getStackInHand(hand).decrement(1);
            }
            world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS);

            return ActionResult.SUCCESS_SERVER;
        } else if (player.getStackInHand(hand).isOf(FactoryItems.TREATED_DRIED_KELP)
                && setColor(state, world, pos, DEFAULT_COLOR)
        ) {
            world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS);

            return ActionResult.SUCCESS_SERVER;
        }

        return super.onUse(state, world, pos, player, hit);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state, boolean includeData) {
        var stack = super.getPickStack(world, pos, state, includeData);
        if (world.getBlockEntity(pos) instanceof ColorProvider be && !be.isDefaultColor()) {
            ColoredItem.setColor(stack, be.getColor());
        }
        return stack;
    }

    public boolean setColor(BlockState state, World world, BlockPos pos, int color) {
        color = FactoryItems.CABLE.downSampleColor(color);
        if (world.getBlockEntity(pos) instanceof ColorProvider provider && provider.getColor() != color) {
            provider.setColor(color);
            var newState = state;
            for (var dir : Direction.values()) {
                var newPos = pos.offset(dir);
                var block = world.getBlockState(newPos);
                newState = newState.with(FACING_PROPERTIES.get(dir), !isDirectionBlocked(state, dir) && canConnectTo(world, provider.getColor(), newPos, block, dir.getOpposite()));
            }
            if (state != newState) {
                world.setBlockState(pos, newState);
            }
            return true;
        }

        return false;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        var hasReceivers = false;
        var hasProviders = false;
        if (world.getBlockEntity(pos) instanceof ColorProvider be) {
            if (itemStack.getItem() instanceof ColoredItem) {
                be.setColor(FactoryItems.CABLE.getItemColor(itemStack));
            }

            var newState = state;
            for (var dir : Direction.values()) {
                var newPos = pos.offset(dir);
                var block = world.getBlockState(newPos);
                if (!isDirectionBlocked(state, dir) && canConnectTo(world, be.getColor(), newPos, block, dir.getOpposite())) {
                    newState = newState.with(FACING_PROPERTIES.get(dir), true);
                    if (placer instanceof ServerPlayerEntity serverPlayer && (!hasReceivers || !hasProviders)) {
                        var net = NetworkComponent.Data.getLogic(serverPlayer.getServerWorld(), newPos);
                        if (net.hasReceivers()) {
                            hasReceivers = true;
                        }

                        if (net.hasProviders()) {
                            hasProviders = true;
                        }
                    }
                }
            }

            if (state != newState) {
                world.setBlockState(pos, newState);
            }
        }
        if (hasReceivers && hasProviders) {
            TriggerCriterion.trigger((ServerPlayerEntity) placer, FactoryTriggers.CABLE_CONNECT);
        }

        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        return isDirectionBlocked(state, direction) ? state : state.with(FACING_PROPERTIES.get(direction), canConnectTo(world, getColor(world, pos), neighborPos, neighborState, direction.getOpposite()));
    }

    public static int getColor(WorldView world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof ColorProvider be ? be.getColor() : DEFAULT_COLOR;
    }

    protected boolean canConnectTo(WorldView world, int ownColor, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        return neighborState.getBlock() instanceof CableConnectable connectable && connectable.canCableConnect(world, ownColor, neighborPos, neighborState, direction);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new SelectiveSideNode(this.getDirections(state)));
    }

    @Override
    public Collection<BlockNode> createEnergyNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new SelectiveSideNode(this.getDirections(state)));
    }

    public EnumSet<Direction> getDirections(BlockState state) {
        var list = new ArrayList<Direction>(6);

        for (var dir : Direction.values()) {
            if (state.get(FACING_PROPERTIES.get(dir)) && !isDirectionBlocked(state, dir)) {
                list.add(dir);
            }
        }

        return list.isEmpty() ? EnumSet.noneOf(Direction.class) : EnumSet.copyOf(list);
    }

    protected boolean isDirectionBlocked(BlockState state, Direction direction) {
        return false;
    }

    @Override
    public boolean canCableConnect(WorldView world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        if (world.getBlockEntity(pos) instanceof ColorProvider be) {
            return be.getColor() == cableColor || be.isDefaultColor() || cableColor == DEFAULT_COLOR;
        }
        return true;
    }

    public boolean hasCable(BlockState state) {
        return true;
    }

    private static boolean checkModelDirection(BlockState state, Direction direction) {
        return state.get(FACING_PROPERTIES.get(direction));
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return FactoryUtil.rotate(state, NORTH, SOUTH, EAST, WEST, rotation);
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return FactoryUtil.mirror(state, NORTH, SOUTH, EAST, WEST, mirror);
    }


    public static class BaseCableModel extends BlockModel {
        private final ItemDisplayElement cable;
        private int color = AbstractCableBlock.DEFAULT_COLOR;
        private BlockState state;

        public BaseCableModel(BlockState state) {
            this.cable = ItemDisplayElementUtil.createSimple();
            this.cable.setViewRange(0.5f);
            this.state = state;
            updateModel();
            if (((AbstractCableBlock) state.getBlock()).hasCable(state)) {
                this.addElement(this.cable);
            }
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE) {
                this.setState(this.blockState());
            }
        }

        protected void setState(BlockState blockState) {
            this.state = blockState;
            if (this.hasCable(state)) {
                this.addElement(this.cable);
            } else {
                this.removeElement(this.cable);
            }
            updateModel();
        }


        protected final boolean hasCable(BlockState state) {
            return ((AbstractCableBlock) state.getBlock()).hasCable(state);
        }

        protected void updateModel() {
            var stack = getModel(this.state, AbstractCableBlock::checkModelDirection);
            stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of(), List.of(), List.of(), IntList.of(this.color)));
            this.cable.setItem(stack);

            if (this.cable.getHolder() == this && this.color >= 0) {
                this.cable.tick();
            }
        }

        public void setColor(int color) {
            this.color = color;
            updateModel();
        }

        public ItemStack getModel(BlockState state, BiPredicate<BlockState, Direction> directionPredicate) {
            return FactoryModels.COLORED_CABLE.get(state, directionPredicate).copy();
        }
    }
}
