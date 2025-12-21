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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;

public abstract class AbstractCableBlock extends AbstracterCableBlock {
    public static final int DEFAULT_COLOR = 0xbbbbbb;
    public static final BooleanProperty HAS_CABLE = BooleanProperty.create("has_cable");

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    public static final Map<Direction, BooleanProperty> FACING_PROPERTIES = FactoryProperties.DIRECTIONS;

    public AbstractCableBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    public boolean setColor(BlockState state, Level world, BlockPos pos, int color) {
        color = FactoryItems.CABLE.downSampleColor(color);
        if (world.getBlockEntity(pos) instanceof ColorProvider provider && provider.getColor() != color) {
            provider.setColor(color);
            var newState = state;
            for (var dir : Direction.values()) {
                var newPos = pos.relative(dir);
                var block = world.getBlockState(newPos);
                newState = newState.setValue(FACING_PROPERTIES.get(dir), !isDirectionBlocked(state, dir) && canConnectTo(world, provider.getColor(), newPos, block, dir.getOpposite()));
            }
            if (state != newState) {
                world.setBlockAndUpdate(pos, newState);
            }
            return true;
        }

        return false;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        var hasReceivers = false;
        var hasProviders = false;
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if (world.getBlockEntity(pos) instanceof ColorProvider be) {
            var newState = state;
            for (var dir : Direction.values()) {
                var newPos = pos.relative(dir);
                var block = world.getBlockState(newPos);
                if (!isDirectionBlocked(state, dir) && canConnectTo(world, be.getColor(), newPos, block, dir.getOpposite())) {
                    newState = newState.setValue(FACING_PROPERTIES.get(dir), true);
                    if (placer instanceof ServerPlayer serverPlayer && (!hasReceivers || !hasProviders)) {
                        var net = NetworkComponent.Data.getLogic(serverPlayer.level(), newPos);
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
                world.setBlockAndUpdate(pos, newState);
            }
        }
        if (hasReceivers && hasProviders) {
            TriggerCriterion.trigger((ServerPlayer) placer, FactoryTriggers.CABLE_CONNECT);
        }

    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return isDirectionBlocked(state, direction) ? state : state.setValue(FACING_PROPERTIES.get(direction), canConnectTo(world, getColor(world, pos), neighborPos, neighborState, direction.getOpposite()));
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new SelectiveSideNode(this.getDirections(state)));
    }

    @Override
    public Collection<BlockNode> createEnergyNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new SelectiveSideNode(this.getDirections(state)));
    }

    public EnumSet<Direction> getDirections(BlockState state) {
        var list = new ArrayList<Direction>(6);

        for (var dir : Direction.values()) {
            if (state.getValue(FACING_PROPERTIES.get(dir)) && !isDirectionBlocked(state, dir)) {
                list.add(dir);
            }
        }

        return list.isEmpty() ? EnumSet.noneOf(Direction.class) : EnumSet.copyOf(list);
    }

    @Override
    protected boolean checkModelDirection(BlockState state, Direction direction) {
        return state.getValue(FACING_PROPERTIES.get(direction));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.rotate(state, NORTH, SOUTH, EAST, WEST, rotation);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FactoryUtil.mirror(state, NORTH, SOUTH, EAST, WEST, mirror);
    }
}
