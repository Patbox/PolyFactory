package eu.pb4.polyfactory.ui.fluid;

import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.ui.GuiTextures;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public record HorizontalFluidTextures(Map<FluidType<?>, char[]> textures, HorizontalFluidUiPositionCreator uiPositionCreator, int width, char backFull, char back) {
    private static final Int2ObjectOpenHashMap<HorizontalFluidUiTextureCreator> TEXTURE_CREATORS = new Int2ObjectOpenHashMap<>();

    public static final HorizontalFluidTextures TOOLTIP = HorizontalFluidTextures.of("tooltip", 16 * 6, 11, -1);

    public Component render(Consumer<BiConsumer<FluidInstance<?>, Float>> provider) {
        var out = Component.empty().setStyle(this.uiPositionCreator.style);

        var line = new MutableInt(0);

        provider.accept((type, amount) -> {
            var b = new StringBuilder();
            var lines = this.textures.get(type.type());
            if (lines == null) {
                return;
            }

            var start = line.getValue();
            var count = Mth.ceil(amount * this.width) + start;

            for (int i = start; i < count; i++) {
                b.append(lines[i % 16]);
                b.append(this.back);
            }

            var t = Component.literal(b.toString());
            if (type.type().color().isPresent()) {
                //noinspection unchecked
                t.withColor(((FluidType.ColorProvider<Object>) type.type().color().get()).getColor(type.data()));
            }

            out.append(t);
            line.setValue(count);
        });

        return out.append(GuiTextures.negativeSpace(line.getValue()));
    }

    public static HorizontalFluidTextures of(String name, int width, int height, int offsetY) {
        TEXTURE_CREATORS.computeIfAbsent(height, (w) -> {
            var creator = new HorizontalFluidUiTextureCreator(w);
            creator.setup();
            return creator;
        });

        var creator = new HorizontalFluidUiPositionCreator(name, height, offsetY);
        var textures = new HorizontalFluidTextures(new IdentityHashMap<>(), creator, width, creator.space(-width - 1), creator.space(-1));
        creator.setup(textures.textures);
        return textures;
    }

    public static void setup() {

    }
}
