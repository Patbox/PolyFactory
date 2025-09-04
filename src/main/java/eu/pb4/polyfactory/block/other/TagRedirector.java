package eu.pb4.polyfactory.block.other;

import net.minecraft.block.Block;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;

public interface TagRedirector {
    boolean customIsIn(TagKey<Block> tagKey, boolean original);

    boolean customIsIn(RegistryEntryList<Block> entryList, boolean original);
}
