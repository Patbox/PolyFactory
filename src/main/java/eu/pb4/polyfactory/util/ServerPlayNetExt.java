package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.item.configuration.WrenchHandler;
import eu.pb4.polyfactory.item.util.MultimeterHandler;

public interface ServerPlayNetExt {
    void polyFactory$resetFloating();
    WrenchHandler polyFactory$getWrenchHandler();
    MultimeterHandler polyFactory$getMultimeterHandler();
}
