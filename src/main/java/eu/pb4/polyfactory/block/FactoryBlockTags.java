package eu.pb4.polyfactory.block;

import eu.pb4.polyfactory.ModInit;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public class FactoryBlockTags {
    public static final TagKey<Block> CONVEYORS = of("conveyors");
    public static final TagKey<Block> CONVEYOR_TOP_OUTPUT = of("conveyor/top_output");
    public static final TagKey<Block> CONVEYOR_SIDE_OUTPUT = of("conveyor/side_output");
    public static final TagKey<Block> GRINDER_TOP_PLACEABLE = of("grinder/top_placeable");

    private static TagKey<Block> of(String path) {
        return TagKey.of(RegistryKeys.BLOCK, ModInit.id(path));
    }
}
