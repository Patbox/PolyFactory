package eu.pb4.polyfactory.models;

import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.property.ConnectablePart;
import eu.pb4.polyfactory.block.property.FactoryProperties;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import static eu.pb4.polyfactory.ModInit.id;

public class MultiElementConnectedModel {
    private final EnumProperty<ConnectablePart> PART_X = FactoryProperties.CONNECTABLE_PART_X;
    private static final EnumProperty<ConnectablePart> PART_Y = FactoryProperties.CONNECTABLE_PART_Y;
    private static final EnumProperty<ConnectablePart> PART_Z = FactoryProperties.CONNECTABLE_PART_Z;
    private final ItemStack[] models = new ItemStack[64];

    public static final Identifier WITH_INNER = id("block/directional_cube_inner");
    private static final Identifier EMPTY = id("block/empty");
    private static final String MODEL_JSON = """
            {
              "parent": "|PARENT|",
              "textures": {
                "west": "|WEST|",
                "east": "|EAST|",
                "north": "|NORTH|",
                "south": "|SOUTH|",
                "up": "|UP|",
                "down": "|DOWN|"
              }
            }
            """;

    private final Identifier base;
    private final Identifier side;
    private final Identifier bottom;
    private final Identifier parent;

    public MultiElementConnectedModel(Identifier base, Identifier side, Identifier bottom, Identifier parent) {
        this.base = base;
        this.side = side;
        this.bottom = bottom;
        this.parent = parent;
        this.models[index(ConnectablePart.MIDDLE, ConnectablePart.MIDDLE, ConnectablePart.MIDDLE)] = ItemStack.EMPTY;

        for (var x : ConnectablePart.values()) {
            for (var y : ConnectablePart.values()) {
                for (var z : ConnectablePart.values()) {
                    var i = index(x, y, z);
                    if (this.models[i] == null) {
                        this.models[i] = ItemDisplayElementUtil.getModel(base.withSuffix("/" + x.getSerializedName() + "_" + y.getSerializedName() + "_" + z.getSerializedName()));
                    }
                }
            }
        }
    }

    public void generateModels(BiConsumer<String, byte[]> dataWriter) {
        for (var x : ConnectablePart.values()) {
            for (var y : ConnectablePart.values()) {
                for (var z : ConnectablePart.values()) {
                    var north = this.side.withSuffix("_" + x.negate().getSerializedName() + "_" + y.getSerializedName());
                    var south = this.side.withSuffix("_" + x.getSerializedName() + "_" + y.getSerializedName());
                    var west = this.side.withSuffix("_" + z.getSerializedName() + "_" + y.getSerializedName());
                    var east = this.side.withSuffix("_" + z.negate().getSerializedName() + "_" + y.getSerializedName());
                    var up = y.middle() || y.negative() ? EMPTY : this.bottom.withSuffix("_" + x.getSerializedName() + "_" + z.negate().getSerializedName());;
                    var down = y.middle() || y.positive() ? EMPTY : this.bottom.withSuffix("_" + x.getSerializedName() + "_" + z.getSerializedName());;

                    if (x.middle()) {
                        west = EMPTY;
                        east = EMPTY;
                    } else if (x.positive()) {
                        west = EMPTY;
                    } else if (x.negative()) {
                        east = EMPTY;
                    }

                    if (z.middle()) {
                        north = EMPTY;
                        south = EMPTY;
                    } else if (z.positive()) {
                        north = EMPTY;
                    } else if (z.negative()) {
                        south = EMPTY;
                    }

                    dataWriter.accept(AssetPaths.model(base.withSuffix("/" + x.getSerializedName() + "_" + y.getSerializedName() + "_" + z.getSerializedName() + ".json")), MODEL_JSON
                            .replace("|PARENT|", this.parent.toString())
                            .replace("|NORTH|", north.toString())
                            .replace("|SOUTH|", south.toString())
                            .replace("|WEST|", west.toString())
                            .replace("|EAST|", east.toString())
                            .replace("|UP|", up.toString())
                            .replace("|DOWN|", down.toString())
                            .getBytes(StandardCharsets.UTF_8)
                    );
                }
            }
        }
    }

    public ItemStack get(BlockState state) {
        var x = state.getValue(PART_X);
        var y = state.getValue(PART_Y);
        var z = state.getValue(PART_Z);
        return get(x, y, z);
    }

    public ItemStack get(ConnectablePart x, ConnectablePart y, ConnectablePart z) {
        return this.models[index(x, y, z)];
    }

    private static int index(ConnectablePart x, ConnectablePart y, ConnectablePart z) {
        return x.ordinal() * 16 + y.ordinal() * 4 + z.ordinal();
    }
}
