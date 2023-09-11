package eu.pb4.polyfactory.item.wrench;

import eu.pb4.polyfactory.block.data.ChannelContainer;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;

import java.util.function.BiConsumer;
import java.util.function.Function;

public record WrenchAction(String id, Text name, WrenchValueGetter value, WrenchApplyAction action) {
    public static final WrenchAction FACING = of("facing", Properties.FACING);
    public static final WrenchAction AXIS = of("axis", Properties.AXIS);
    public static final WrenchAction FACING_HORIZONTAL = of("facing", Properties.HORIZONTAL_FACING);
    public static final WrenchAction HALF = of("half", Properties.BLOCK_HALF);
    public static final WrenchAction CHANNEL = ofBlockEntity("channel",
            ChannelContainer.class,
            x -> String.valueOf(x.channel()),
            (x, n) -> x.setChannel((x.channel() + 4 + (n ? 1 : -1)) % 4 )
    );

    public static WrenchAction of(String id, Property<?> property) {
        return new WrenchAction(id, Text.translatable("item.polyfactory.wrench.action." + id),
                WrenchValueGetter.ofProperty(property), WrenchApplyAction.ofProperty(property));
    }

    public static <T> WrenchAction ofBlockEntity(String id, Class<T> tClass, Function<T, String> value, BiConsumer<T, Boolean> change) {
        return new WrenchAction(id, Text.translatable("item.polyfactory.wrench.action." + id),
                WrenchValueGetter.ofBlockEntity(tClass, value), WrenchApplyAction.ofBlockEntity(tClass, change));
    }
}
