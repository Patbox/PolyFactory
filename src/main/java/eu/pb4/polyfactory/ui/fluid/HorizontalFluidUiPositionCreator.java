package eu.pb4.polyfactory.ui.fluid;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.chars.Char2IntMap;
import it.unimi.dsi.fastutil.chars.Char2IntOpenHashMap;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class HorizontalFluidUiPositionCreator {
    public final Style style;
    private final Char2IntMap spaces = new Char2IntOpenHashMap();
    private final List<UiResourceCreator.FontTexture> fontTextures = new ArrayList<>();
    private final int height;
    private final int offsetY;
    private final Identifier id;
    private final String name;
    private char character = 256;

    public HorizontalFluidUiPositionCreator(String name, int height, int offsetY) {
        this.id = id("fluid_h/" + name);
        this.name = name;
        this.height = height;
        this.offsetY = offsetY;
        this.style = Style.EMPTY.withColor(0xFFFFFF).withFont(new FontDescription.Resource(this.id));
    }

    public char[] registerTextures(Identifier identifier) {
        identifier = identifier.withPrefix("gen/fluids_1_h" + this.height + "/");
        var chars = new char[16];

        for (int i = 0; i < 16; i++) {
            chars[i] = character++;
        }

        var texture = new UiResourceCreator.FontTexture(identifier, 13 - height - offsetY, height, new char[][] { chars });
        fontTextures.add(texture);

        return chars;
    }

    public char space(int width) {
        var c = character++;
        spaces.put(c, width);
        return c;
    }

    public void setup(Map<FluidType<?>, char[]> textures) {
        for (var fluid : FactoryRegistries.FLUID_TYPES.keySet()) {
            textures.put(FactoryRegistries.FLUID_TYPES.getValue(fluid), this.registerTextures(fluid));
        }

        RegistryEntryAddedCallback.event(FactoryRegistries.FLUID_TYPES).register((rawId, id, object) -> {
            textures.put(object, this.registerTextures(id));
        });

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register((b) -> this.generateAssets(b::addData));
    }

    public void generateAssets(BiConsumer<String, byte[]> assetWriter) {
        var fontBase = new JsonObject();
        var providers = new JsonArray();

        {
            var spaces = new JsonObject();
            spaces.addProperty("type", "space");
            var advances = new JsonObject();
            this.spaces.char2IntEntrySet().stream().sorted(Comparator.comparing(Char2IntMap.Entry::getCharKey)).forEach((c) -> advances.addProperty(Character.toString(c.getCharKey()), c.getIntValue()));
            spaces.add("advances", advances);
            providers.add(spaces);
        }

        fontTextures.forEach((entry) -> {
            var bitmap = new JsonObject();
            bitmap.addProperty("type", "bitmap");
            bitmap.addProperty("file", entry.path() + ".png");
            bitmap.addProperty("ascent", entry.ascent());
            bitmap.addProperty("height", entry.height());
            var chars = new JsonArray();

            for (var a : entry.chars()) {
                var builder = new StringBuilder();
                for (var b : a) {
                    builder.append(b);
                }
                chars.add(builder.toString());
            }

            bitmap.add("chars", chars);
            providers.add(bitmap);
        });

        fontBase.add("providers", providers);

        assetWriter.accept("assets/polyfactory/font/fluid_h/" + this.name + ".json", fontBase.toString().getBytes(StandardCharsets.UTF_8));
    }
}
