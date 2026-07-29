package eu.pb4.polyfactory.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

public class OtherBlockIds {
    // Enderscape
    public static final ResourceKey<Block> VOID_LACHRYMA = of("enderscape", "void_lachryma");


    private static ResourceKey<Block> of(String namespace, String path) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(namespace, path));
    }
}
