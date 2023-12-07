package eu.pb4.factorytools.api.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.util.Identifier;

public interface RegistryCallbackItem {
    void onRegistered(Identifier selfId);
}
