package eu.pb4.polyfactory.item.wrench;

import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;

public record WrenchAction(String id, Text name, WrenchValueGetter value, WrenchApplyAction action) {
    public static final WrenchAction FACING = of("facing", Properties.FACING);
    public static final WrenchAction AXIS = of("axis", Properties.AXIS);
    public static final WrenchAction FACING_HORIZONTAL = of("facing", Properties.HORIZONTAL_FACING);
    public static final WrenchAction HALF = of("half", Properties.BLOCK_HALF);

    public static WrenchAction of(String id, Property<?> property) {
        return new WrenchAction(id, Text.translatable("item.polyfactory.wrench.action." + id),
                WrenchValueGetter.ofProperty(property), WrenchApplyAction.ofProperty(property));
    }
}
