package eu.pb4.polyfactory.block.base;

import net.minecraft.world.chunk.WorldChunk;

public interface BlockEntityExtraListener {
    void onListenerUpdate(WorldChunk chunk);
}
