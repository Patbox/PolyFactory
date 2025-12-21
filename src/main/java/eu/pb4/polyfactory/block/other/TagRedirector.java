package eu.pb4.polyfactory.block.other;

import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public interface TagRedirector {
    boolean customIsIn(TagKey<Block> tagKey, boolean original);

    boolean customIsIn(HolderSet<Block> entryList, boolean original);
}
