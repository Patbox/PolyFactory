package eu.pb4.polyfactory.block.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.CableModel;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.nodes.generic.SelectiveSideNode;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.List;

public abstract class AbstractCableBlock extends NetworkBlock implements FactoryBlock, BlockEntityProvider, CableConnectable, NetworkComponent.Data, NetworkComponent.Energy {
    public static final int DEFAULT_COLOR = 0xbbbbbb;
    public static final BooleanProperty HAS_CABLE = BooleanProperty.of("has_cable");

    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty WEST = Properties.WEST;
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty DOWN = Properties.DOWN;
    public static final Map<Direction, BooleanProperty> FACING_PROPERTIES = ImmutableMap.copyOf((Map) Util.make(Maps.newEnumMap(Direction.class), (directions) -> {
        directions.put(Direction.NORTH, NORTH);
        directions.put(Direction.EAST, EAST);
        directions.put(Direction.SOUTH, SOUTH);
        directions.put(Direction.WEST, WEST);
        directions.put(Direction.UP, UP);
        directions.put(Direction.DOWN, DOWN);
    }));

    public AbstractCableBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(NORTH, false).with(SOUTH, false)
                .with(EAST, false).with(WEST, false).with(UP, false).with(DOWN, false));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.getStackInHand(hand).isIn(ConventionalItemTags.DYES)
                && setColor(state, world, pos, FactoryItems.CABLE.downSampleColor(DyeColorExtra.getColor(player.getStackInHand(hand))))
        ) {
            if (!player.isCreative()) {
                player.getStackInHand(hand).decrement(1);
            }
            world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS);

            return ActionResult.SUCCESS;
        } else if (player.getStackInHand(hand).isOf(FactoryItems.TREATED_DRIED_KELP)
                && setColor(state, world, pos, DEFAULT_COLOR)
        ) {
            world.playSound(null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS);

            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        var stack = super.getPickStack(world, pos, state);
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
    protected void updateNetworkAt(WorldAccess world, BlockPos pos) {
        NetworkComponent.Data.updateDataAt(world, pos);
        NetworkComponent.Energy.updateEnergyAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof NetworkComponent.Data || block instanceof NetworkComponent.Energy;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return isDirectionBlocked(state, direction) ? state : state.with(FACING_PROPERTIES.get(direction), canConnectTo(world, getColor(world, pos), neighborPos, neighborState, direction.getOpposite()));
    }

    public static int getColor(WorldAccess world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof ColorProvider be ? be.getColor() : DEFAULT_COLOR;
    }

    protected boolean canConnectTo(WorldAccess world, int ownColor, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        return neighborState.getBlock() instanceof CableConnectable connectable && connectable.canCableConnect(world, ownColor, neighborPos, neighborState, direction);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new SelectiveSideNode(this.getDirections(state)));
    }

    @Override
    public Collection<BlockNode> createEnergyNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return createDataNodes(state, world, pos);
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

    public static int getModelId(BlockState state) {
        int i = 0;

        for(int j = 0; j < Direction.values().length; ++j) {
            var direction = Direction.values()[j];
            if (state.get(FACING_PROPERTIES.get(direction))) {
                i |= 1 << j;
            }
        }

        return i;
    }

    public static boolean hasDirection(int i, Direction direction) {
        if (direction == null) {
            return false;
        }

        return (i & (1 << direction.ordinal())) != 0;
    }

    @Override
    public boolean canCableConnect(WorldAccess world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        if (world.getBlockEntity(pos) instanceof ColorProvider be) {
            return be.getColor() == cableColor || be.isDefaultColor() || cableColor == DEFAULT_COLOR;
        }
        return true;
    }

    public boolean hasCable(BlockState state) {
        return true;
    }

    public static class BaseCableModel extends BlockModel {
        private final ItemDisplayElement cable;
        private int color = AbstractCableBlock.DEFAULT_COLOR;
        private BlockState state;

        public BaseCableModel(BlockState state, boolean addCable) {
            this.cable = ItemDisplayElementUtil.createSimple();
            this.cable.setViewRange(0.5f);
            this.state = state;
            updateModel();
            if (addCable) {
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
            var stack = CableModel.MODELS_BY_ID[getModelId(state)].copy();
            var display = new NbtCompound();
            display.putInt("color", this.color);
            stack.getOrCreateNbt().put("display", display);
            this.cable.setItem(stack);
            if (this.cable.getHolder() == this && this.color >= 0) {
                this.cable.tick();
            }
        }

        public void setColor(int color) {
            this.color = color;
            updateModel();
        }
    }
}
