package eu.pb4.factorytools.api.block;

import eu.pb4.factorytools.api.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;

public interface FactoryBlock extends PolymerBlock, BlockWithElementHolder, VirtualDestroyStage.Marker {
}
