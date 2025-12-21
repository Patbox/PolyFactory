package eu.pb4.polyfactory.block.property;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import java.util.Map;

public interface FactoryProperties {
    LazyEnumProperty<TriState> TRI_STATE_NORTH = LazyEnumProperty.of("north", TriState.class);
    LazyEnumProperty<TriState> TRI_STATE_SOUTH = LazyEnumProperty.of("south", TriState.class);
    LazyEnumProperty<TriState> TRI_STATE_EAST = LazyEnumProperty.of("east", TriState.class);
    LazyEnumProperty<TriState> TRI_STATE_WEST = LazyEnumProperty.of("west", TriState.class);
    LazyEnumProperty<TriState> TRI_STATE_UP = LazyEnumProperty.of("up", TriState.class);
    LazyEnumProperty<TriState> TRI_STATE_DOWN = LazyEnumProperty.of("down", TriState.class);

    EnumProperty<ConnectablePart> CONNECTABLE_PART_X = EnumProperty.create("part_x", ConnectablePart.class);
    EnumProperty<ConnectablePart> CONNECTABLE_PART_Y = EnumProperty.create("part_y", ConnectablePart.class);
    EnumProperty<ConnectablePart> CONNECTABLE_PART_Z = EnumProperty.create("part_z", ConnectablePart.class);

    Map<Direction, BooleanProperty> DIRECTIONS = ImmutableMap.copyOf((Map) Util.make(Maps.newEnumMap(Direction.class), (directions) -> {
        directions.put(Direction.NORTH, BlockStateProperties.NORTH);
        directions.put(Direction.EAST, BlockStateProperties.EAST);
        directions.put(Direction.SOUTH, BlockStateProperties.SOUTH);
        directions.put(Direction.WEST, BlockStateProperties.WEST);
        directions.put(Direction.UP, BlockStateProperties.UP);
        directions.put(Direction.DOWN, BlockStateProperties.DOWN);
    }));

    Map<Direction, LazyEnumProperty<TriState>> TRI_STATE_DIRECTIONS = ImmutableMap.copyOf((Map) Util.make(Maps.newEnumMap(Direction.class), (directions) -> {
        directions.put(Direction.NORTH, TRI_STATE_NORTH);
        directions.put(Direction.EAST, TRI_STATE_EAST);
        directions.put(Direction.SOUTH, TRI_STATE_SOUTH);
        directions.put(Direction.WEST, TRI_STATE_WEST);
        directions.put(Direction.UP, TRI_STATE_UP);
        directions.put(Direction.DOWN, TRI_STATE_DOWN);
    }));


    BooleanProperty FIRST_AXIS = BooleanProperty.create("first_axis");
    BooleanProperty READ_ONLY = BooleanProperty.create("read_only");
    BooleanProperty ACTIVE = BooleanProperty.create("active");
    IntegerProperty FRONT = IntegerProperty.create("front", 0, 3);
    BooleanProperty POSITIVE_CONNECTED = BooleanProperty.create("positive_connected");
    BooleanProperty NEGATIVE_CONNECTED = BooleanProperty.create("negative_connected");
    EnumProperty<Direction> HORIZONTAL_DIRECTION = EnumProperty.create("direction", Direction.class, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    BooleanProperty LOCKED = BooleanProperty.create("locked");
    BooleanProperty REVERSE = BooleanProperty.create("reverse");

}
