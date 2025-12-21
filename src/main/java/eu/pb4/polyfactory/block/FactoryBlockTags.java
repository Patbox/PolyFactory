package eu.pb4.polyfactory.block;

import eu.pb4.polyfactory.ModInit;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class FactoryBlockTags {
    public static final TagKey<Block> CONVEYORS = of("conveyors");
    public static final TagKey<Block> CONVEYOR_TOP_OUTPUT = of("conveyor/top_output");
    public static final TagKey<Block> CONVEYOR_SIDE_OUTPUT = of("conveyor/side_output");
    public static final TagKey<Block> SPLITTER_SIDE_OUTPUT = of("conveyor/splitter/side_output");
    public static final TagKey<Block> GRINDER_TOP_PLACEABLE = of("grinder/top_placeable");
    public static final TagKey<Block> WIND_PASSTHROUGH = of("wind_passthrough");
    public static final TagKey<Block> SPRAY_CAN_COLORABLE = of("spray_can_colorable");
    public static final TagKey<Block> CONCRETE = of("concrete");
    public static final TagKey<Block> PIPE_CONNECTABLE = of("pipe_connectable");
    public static final TagKey<Block> UNCONNECTABLE = of("unconnectable");

    private static TagKey<Block> of(String path) {
        return TagKey.create(Registries.BLOCK, ModInit.id(path));
    }
}
