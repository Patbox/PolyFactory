package eu.pb4.polyfactory.block.base;

import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;

public interface FactoryBlock extends PolymerBlock, BlockWithElementHolder, VirtualDestroyStage.Marker {
}
