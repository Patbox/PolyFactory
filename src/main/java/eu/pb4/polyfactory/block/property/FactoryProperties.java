package eu.pb4.polyfactory.block.property;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.state.property.*;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;

import java.util.Map;

public interface FactoryProperties {
    LazyEnumProperty<TriState> TRI_STATE_NORTH = LazyEnumProperty.of("north", TriState.class);
    LazyEnumProperty<TriState> TRI_STATE_SOUTH = LazyEnumProperty.of("south", TriState.class);
    LazyEnumProperty<TriState> TRI_STATE_EAST = LazyEnumProperty.of("east", TriState.class);
    LazyEnumProperty<TriState> TRI_STATE_WEST = LazyEnumProperty.of("west", TriState.class);
    LazyEnumProperty<TriState> TRI_STATE_UP = LazyEnumProperty.of("up", TriState.class);
    LazyEnumProperty<TriState> TRI_STATE_DOWN = LazyEnumProperty.of("down", TriState.class);

    EnumProperty<ConnectablePart> CONNECTABLE_PART_X = EnumProperty.of("part_x", ConnectablePart.class);
    EnumProperty<ConnectablePart> CONNECTABLE_PART_Y = EnumProperty.of("part_y", ConnectablePart.class);
    EnumProperty<ConnectablePart> CONNECTABLE_PART_Z = EnumProperty.of("part_z", ConnectablePart.class);

    Map<Direction, BooleanProperty> DIRECTIONS = ImmutableMap.copyOf((Map) Util.make(Maps.newEnumMap(Direction.class), (directions) -> {
        directions.put(Direction.NORTH, Properties.NORTH);
        directions.put(Direction.EAST, Properties.EAST);
        directions.put(Direction.SOUTH, Properties.SOUTH);
        directions.put(Direction.WEST, Properties.WEST);
        directions.put(Direction.UP, Properties.UP);
        directions.put(Direction.DOWN, Properties.DOWN);
    }));

    Map<Direction, LazyEnumProperty<TriState>> TRI_STATE_DIRECTIONS = ImmutableMap.copyOf((Map) Util.make(Maps.newEnumMap(Direction.class), (directions) -> {
        directions.put(Direction.NORTH, TRI_STATE_NORTH);
        directions.put(Direction.EAST, TRI_STATE_EAST);
        directions.put(Direction.SOUTH, TRI_STATE_SOUTH);
        directions.put(Direction.WEST, TRI_STATE_WEST);
        directions.put(Direction.UP, TRI_STATE_UP);
        directions.put(Direction.DOWN, TRI_STATE_DOWN);
    }));


    BooleanProperty FIRST_AXIS = BooleanProperty.of("first_axis");
    BooleanProperty READ_ONLY = BooleanProperty.of("read_only");
    BooleanProperty ACTIVE = BooleanProperty.of("active");
    IntProperty FRONT = IntProperty.of("front", 0, 3);
    BooleanProperty POSITIVE_CONNECTED = BooleanProperty.of("positive_connected");
    BooleanProperty NEGATIVE_CONNECTED = BooleanProperty.of("negative_connected");
    DirectionProperty HORIZONTAL_DIRECTION = DirectionProperty.of("direction", Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
}
