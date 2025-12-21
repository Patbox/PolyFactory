package eu.pb4.polyfactory.block.mechanical;

import net.minecraft.util.Mth;

public class RotationConstants {
    public static final float MAX_ROTATION_PER_TICK_1 = Mth.PI / 1f - Mth.EPSILON;
    public static final float MAX_ROTATION_PER_TICK_2 = Mth.PI / 2f - Mth.EPSILON;
    public static final float MAX_ROTATION_PER_TICK_3 = Mth.PI / 3f - Mth.EPSILON;
    public static final float MAX_ROTATION_PER_TICK_4 = Mth.PI / 4f - Mth.EPSILON;
    public static final float MAX_SPEED = 180;

    public static final int VISUAL_UPDATE_RATE = 3;
    public static final float VISUAL_MAX_ROTATION = MAX_ROTATION_PER_TICK_3;

    public static final double HAND_CRANK_SPEED = 11;
    public static final double HAND_CRANK_STRESS = 18;
}
