package eu.pb4.polyfactory.util;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

import java.util.function.Supplier;

public enum RedstoneActivationType implements StringRepresentable {
    ALWAYS("always", GuiTextures.BUTTON_ACTIVE_ALWAYS, x -> true),
    POWERED("powered", GuiTextures.BUTTON_ACTIVE_POWERED, x -> x),
    NOT_POWERED("not_powered", GuiTextures.BUTTON_ACTIVE_NOT_POWERED, x -> !x);

    public static final Codec<RedstoneActivationType> CODEC = StringRepresentable.fromEnum(RedstoneActivationType::values);

    private final String name;
    private final Supplier<GuiElementBuilder> button;
    private final BoolPredicate predicate;

    RedstoneActivationType(String name, Supplier<GuiElementBuilder> button, BoolPredicate predicate) {
        this.name = name;
        this.button = button;
        this.predicate = predicate;
    }

    public GuiElementBuilder createButton() {
        return this.button.get().setName(asName()).addLoreLine(this.asDescription().withStyle(ChatFormatting.GRAY));
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public MutableComponent asName() {
        return Component.translatable("text.polyfactory.redstone_activation." + this.name);
    }

    public MutableComponent asDescription() {
        return Component.translatable("text.polyfactory.redstone_activation." + this.name + ".desc");
    }

    public boolean isActive(boolean powered) {
        return this.predicate.check(powered);
    }

    interface BoolPredicate {
        boolean check(boolean isPowered);
    }
}
