package eu.pb4.polyfactory.block.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.models.CableModel;
import eu.pb4.polyfactory.models.LodItemDisplayElement;
import eu.pb4.polyfactory.nodes.generic.SelectiveSideNode;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CableBlock extends DataNetworkBlock implements PolymerBlock, VirtualDestroyStage.Marker, BlockWithElementHolder {
    public static final BooleanProperty NORTH;
    public static final BooleanProperty EAST;
    public static final BooleanProperty SOUTH;
    public static final BooleanProperty WEST;
    public static final BooleanProperty UP;
    public static final BooleanProperty DOWN;
    public static final Map<Direction, BooleanProperty> FACING_PROPERTIES;

    public CableBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(NORTH, false).with(SOUTH, false)
                .with(EAST, false).with(WEST, false).with(UP, false).with(DOWN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING_PROPERTIES.get(ctx.getSide().getOpposite()), true);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new SelectiveSideNode(this.getDirections(state)));
    }

    public EnumSet<Direction> getDirections(BlockState state) {
        var list = new ArrayList<Direction>(6);

        for (var dir : Direction.values()) {
            if (state.get(FACING_PROPERTIES.get(dir))) {
                list.add(dir);
            }
        }

        return EnumSet.copyOf(list);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.STRUCTURE_VOID;
    }

    public static int getModelId(BlockState state) {
        int i = 0;

        for(int j = 0; j < Direction.values().length; ++j) {
            if (state.get(FACING_PROPERTIES.get(Direction.values()[j]))) {
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
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    static {
        NORTH = Properties.NORTH;
        EAST = Properties.EAST;
        SOUTH = Properties.SOUTH;
        WEST = Properties.WEST;
        UP = Properties.UP;
        DOWN = Properties.DOWN;
        FACING_PROPERTIES = ImmutableMap.copyOf((Map) Util.make(Maps.newEnumMap(Direction.class), (directions) -> {
            directions.put(Direction.NORTH, NORTH);
            directions.put(Direction.EAST, EAST);
            directions.put(Direction.SOUTH, SOUTH);
            directions.put(Direction.WEST, WEST);
            directions.put(Direction.UP, UP);
            directions.put(Direction.DOWN, DOWN);
        }));
    }

    public final class Model extends BaseModel {
        private final ItemDisplayElement main;

        private Model(BlockState state) {
            this.main = LodItemDisplayElement.createSimple();
            this.main.setViewRange(0.5f);
            this.setState(state);
            this.addElement(this.main);
        }


        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.setState(BlockBoundAttachment.get(this).getBlockState());
                this.tick();
            }
        }

        private void setState(BlockState blockState) {
            this.main.setItem(CableModel.MODELS_BY_ID[getModelId(blockState)]);
        }
    }
}
