package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.item.wrench.WrenchHandler;

public interface ServerPlayNetExt {
    void polyFactory$resetFloating();
    VirtualDestroyStage polyFactory$getVirtualDestroyStage();
    WrenchHandler polyFactory$getWrenchHandler();
}
