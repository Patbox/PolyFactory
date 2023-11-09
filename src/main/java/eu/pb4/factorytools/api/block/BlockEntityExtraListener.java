package eu.pb4.factorytools.api.block;

import net.minecraft.world.chunk.WorldChunk;

public interface BlockEntityExtraListener {
    void onListenerUpdate(WorldChunk chunk);
}
