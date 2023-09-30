package eu.pb4.polyfactory.block.mechanical;

import net.minecraft.util.math.MathHelper;

public class RotationConstants {
    public static final float MAX_ROTATION_PER_TICK_1 = MathHelper.PI / 1f - MathHelper.EPSILON;
    public static final float MAX_ROTATION_PER_TICK_2 = MathHelper.PI / 2f - MathHelper.EPSILON;
    public static final float MAX_ROTATION_PER_TICK_3 = MathHelper.PI / 3f - MathHelper.EPSILON;
    public static final float MAX_ROTATION_PER_TICK_4 = MathHelper.PI / 4f - MathHelper.EPSILON;
    public static final float MAX_SPEED = 180;

    public static final int VISUAL_UPDATE_RATE = 3;
    public static final float VISUAL_MAX_ROTATION = MAX_ROTATION_PER_TICK_3;

    public static final double HAND_CRANK_SPEED = 11;
    public static final double HAND_CRANK_STRESS = 18;
}
