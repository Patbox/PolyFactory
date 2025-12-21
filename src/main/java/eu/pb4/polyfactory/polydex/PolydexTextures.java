package eu.pb4.polyfactory.polydex;

import static eu.pb4.polyfactory.ui.UiResourceCreator.polydexBackground;

import net.minecraft.network.chat.Component;


public class PolydexTextures {
    public static final Component GRINDER;
    public static final Component PRESS;
    public static final Component PRESS_FLUID;
    public static final Component MIXER;
    public static final Component DRAIN;
    public static final Component SPOUT;
    public static final Component SMELTERY;

    public static void register() {

    }

    static {
        var t0 = polydexBackground("0");
        var t1 = polydexBackground("1");
        var t2 = polydexBackground("2");
        var t3 = polydexBackground("3");
        PRESS = t0.getA();
        MIXER = t0.getB();
        GRINDER = t1.getA();
        SMELTERY = t1.getB();
        DRAIN = t2.getA();
        SPOUT = t2.getB();
        PRESS_FLUID = t3.getA();
    }
}
