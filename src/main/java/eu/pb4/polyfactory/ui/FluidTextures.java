package eu.pb4.polyfactory.ui;

import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidType;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public record FluidTextures(Map<FluidType<?>, char[]> textures, FluidUiPositionCreator uiPositionCreator, char back) {
    private static final Int2ObjectOpenHashMap<FluidUiTextureCreator> TEXTURE_CREATORS = new Int2ObjectOpenHashMap<>();

    public static final FluidTextures MIXER = FluidTextures.of("mixer", 10, 48, 19);
    public static final FluidTextures MIXER_POLYDEX = FluidTextures.of("mixer_polydex", 10, 48, 19 + 18);
    public static final FluidTextures SMELTERY = FluidTextures.of("smeltery", 48, 80, 21);


    public Text render(Consumer<BiConsumer<FluidInstance<?>, Float>> provider) {
        var out = Text.empty().setStyle(this.uiPositionCreator.style);

        var line = new MutableInt(0);

        provider.accept((type, amount) -> {
            var b = new StringBuilder();
            var lines = this.textures.get(type.type());
            if (lines == null) {
                return;
            }

            var start = line.getValue();
            var count = Math.min(MathHelper.ceil(amount * this.uiPositionCreator.height()) + start, lines.length);

            for (int i = start; i < count; i++) {
                b.append(lines[i]);
                b.append(this.back);
            }

            var t = Text.literal(b.toString());
            if (type.type().color().isPresent()) {
                //noinspection unchecked
                t.withColor(((FluidType.ColorProvider<Object>) type.type().color().get()).getColor(type.data()));
            }

            out.append(t);
            line.setValue(count);
        });

        return out;
    }

    public static FluidTextures of(String name, int width, int height, int offsetY) {
        TEXTURE_CREATORS.computeIfAbsent(width, (w) -> {
            var creator = new FluidUiTextureCreator(w);
            creator.setup();
            return creator;
        });

        var creator = new FluidUiPositionCreator(name, width, height, offsetY);
        var textures = new FluidTextures(new IdentityHashMap<>(), creator, creator.space(-width - 1));
        creator.setup(textures.textures);
        return textures;
    }

    public static void setup() {

    }
}
