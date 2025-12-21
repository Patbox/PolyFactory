package eu.pb4.polyfactory.block.property;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum ValueModifier implements StringRepresentable {
    ABSOLUTE("absolute", Math::abs),
    NEGATED_ABSOLUTE("negated_absolute", x -> -Math.abs(x)),
    UNMODIFIED("unmodified", x -> x),
    NEGATED("negated", x -> -x),
    ;

    private final String name;
    private final Float2FloatFunction function;

    ValueModifier(String name, Float2FloatFunction function) {
        this.name = name;
        this.function = function;
    }

    public Component text() {
        return Component.translatable("item.polyfactory.wrench.action.value_modifier." + this.name);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public float apply(float val) {
        return this.function.apply(val);
    }

    private interface Float2FloatFunction {
        float apply(float val);
    }
}