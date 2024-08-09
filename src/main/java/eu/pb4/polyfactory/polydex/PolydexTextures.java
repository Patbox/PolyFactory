package eu.pb4.polyfactory.polydex;

import net.minecraft.text.Text;

import static eu.pb4.polyfactory.ui.UiResourceCreator.polydexBackground;


public class PolydexTextures {
    public static final Text GRINDER;
    public static final Text PRESS;
    public static final Text MIXER;
    public static final Text DRAIN;
    public static final Text SPOUT;

    public static void register() {

    }

    static {
        var t0 = polydexBackground("0");
        var t1 = polydexBackground("1");
        var t2 = polydexBackground("2");
        PRESS = t0.getLeft();
        MIXER = t0.getRight();
        GRINDER = t1.getLeft();
        DRAIN = t2.getLeft();
        SPOUT = t2.getRight();
    }
}
