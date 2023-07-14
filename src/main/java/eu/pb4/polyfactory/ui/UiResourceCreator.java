package eu.pb4.polyfactory.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.util.ResourceUtils;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import it.unimi.dsi.fastutil.chars.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class UiResourceCreator {
    public static final String BASE_MODEL = "minecraft:item/generated";
    public static final String X32_MODEL = "polyfactory:sgui/button_32";
    public static final String X32_RIGHT_MODEL = "polyfactory:sgui/button_32_right";

    private static final Style STYLE = Style.EMPTY.withColor(0xFFFFFF).withFont(id("gui"));
    private static final String ITEM_TEMPLATE = """
            {
              "parent": "|BASE|",
              "textures": {
                "layer0": "|ID|"
              }
            }
            """.replace(" ", "").replace("\n", "");

    private static final List<SlicedTexture> VERTICAL_PROGRESS = new ArrayList<>();
    private static final List<SlicedTexture> HORIZONTAL_PROGRESS = new ArrayList<>();
    private static List<Pair<PolymerModelData, String>> SIMPLE_MODEL = new ArrayList<>();
    private static Char2IntMap SPACES = new Char2IntOpenHashMap();
    private static Char2ObjectMap<Identifier> TEXTURES = new Char2ObjectOpenHashMap<>();
    private static char character = 'a';

    private static final char CHEST_SPACE0 = character++;
    private static final char CHEST_SPACE1 = character++;

    public static Supplier<GuiElementBuilder> icon16(String path) {
        var model = genericIconRaw(Items.ALLIUM, path, BASE_MODEL);
        return () -> new GuiElementBuilder(model.item()).setName(Text.empty()).hideFlags().setCustomModelData(model.value());
    }

    public static Supplier<GuiElementBuilder> icon32(String path) {
        var model = genericIconRaw(Items.ALLIUM, path, X32_MODEL);
        return () -> new GuiElementBuilder(model.item()).setName(Text.empty()).hideFlags().setCustomModelData(model.value());
    }

    public static IntFunction<GuiElementBuilder> icon16(String path, int size) {
        var models = new PolymerModelData[size];

        for (var i = 0; i < size; i++) {
            models[i] = genericIconRaw(Items.ALLIUM, path + "_" + i, BASE_MODEL);
        }
        return (i) -> new GuiElementBuilder(models[i].item()).setName(Text.empty()).hideFlags().setCustomModelData(models[i].value());
    }

    public static IntFunction<GuiElementBuilder> horizontalProgress16(String path, int start, int stop, boolean reverse) {
        return genericProgress(path, start, stop, reverse, BASE_MODEL, HORIZONTAL_PROGRESS);
    }

    public static IntFunction<GuiElementBuilder> horizontalProgress32(String path, int start, int stop, boolean reverse) {
        return genericProgress(path, start, stop, reverse, X32_MODEL, HORIZONTAL_PROGRESS);
    }

    public static IntFunction<GuiElementBuilder> horizontalProgress32Right(String path, int start, int stop, boolean reverse) {
        return genericProgress(path, start, stop, reverse, X32_RIGHT_MODEL, HORIZONTAL_PROGRESS);
    }

    public static IntFunction<GuiElementBuilder> verticalProgress32(String path, int start, int stop, boolean reverse) {
        return genericProgress(path, start, stop, reverse, X32_MODEL, VERTICAL_PROGRESS);
    }

    public static IntFunction<GuiElementBuilder> verticalProgress32Right(String path, int start, int stop, boolean reverse) {
        return genericProgress(path, start, stop, reverse, X32_RIGHT_MODEL, VERTICAL_PROGRESS);
    }

    public static IntFunction<GuiElementBuilder> verticalProgress16(String path, int start, int stop, boolean reverse) {
        return genericProgress(path, start, stop, reverse, BASE_MODEL, VERTICAL_PROGRESS);
    }

    public static IntFunction<GuiElementBuilder> genericProgress(String path, int start, int stop, boolean reverse, String base, List<SlicedTexture> progressType) {

        var models = new PolymerModelData[stop - start];

        progressType.add(new SlicedTexture(path, start, stop, reverse));

        for (var i = start; i < stop; i++) {
            models[i - start] = genericIconRaw(Items.ALLIUM,  "gen/" + path + "_" + i, base);
        }
        return (i) -> new GuiElementBuilder(models[i].item()).setName(Text.empty()).hideFlags().setCustomModelData(models[i].value());
    }


    public static PolymerModelData genericIconRaw(Item item, String path, String base) {
        var model = PolymerResourcePackUtils.requestModel(item, elementPath(path));
        SIMPLE_MODEL.add(new Pair<>(model, base));
        return model;
    }

    private static Identifier elementPath(String path) {
        return id("sgui/elements/" + path);
    }

    public static Function<Text, Text> background(String path) {
        var builder = new StringBuilder().append(CHEST_SPACE0);
        var c = (character++);
        builder.append(c);
        builder.append(CHEST_SPACE1);
        TEXTURES.put(c, id("sgui/" + path));

        return new TextBuilders(Text.literal(builder.toString()).setStyle(STYLE));
    }

    public static void setup() {
        SPACES.put(CHEST_SPACE0, -8);
        SPACES.put(CHEST_SPACE1, -168);

        if (ModInit.DYNAMIC_ASSETS) {
            PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register((b) -> UiResourceCreator.generateAssets(b::addData));
        }
    }

    private static void generateProgress(BiConsumer<String, byte[]> assetWriter, List<SlicedTexture> list, boolean horizontal) {
        for (var pair : list) {
            var sourceImage = ResourceUtils.getTexture(elementPath(pair.path()));

            var image = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

            var xw = horizontal ? image.getHeight() : image.getWidth();

            var mult = pair.reverse ? -1 : 1;
            var offset = pair.reverse ? pair.stop + pair.start - 1 : 0;

            for (var y = pair.start; y < pair.stop; y++) {
                var path = elementPath("gen/" + pair.path + "_" + y);
                var pos = offset + y * mult;

                for (var x = 0; x < xw; x++) {
                    if (horizontal) {
                        image.setRGB(pos, x, sourceImage.getRGB(pos, x));
                    } else {
                        image.setRGB(x, pos, sourceImage.getRGB(x, pos));
                    }
                }

                var out = new ByteArrayOutputStream();
                try {
                    ImageIO.write(image, "png", out);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                assetWriter.accept(AssetPaths.texture(path.getNamespace(), path.getPath() + ".png"), out.toByteArray());
            }
        }
    }

    public static void generateAssets(BiConsumer<String, byte[]> assetWriter) {
        for (var texture : SIMPLE_MODEL) {
            assetWriter.accept("assets/" + texture.getLeft().modelPath().getNamespace() + "/models/" + texture.getLeft().modelPath().getPath() + ".json",
                    ITEM_TEMPLATE.replace("|ID|", texture.getLeft().modelPath().toString()).replace("|BASE|", texture.getRight()).getBytes(StandardCharsets.UTF_8));
        }

        generateProgress(assetWriter, VERTICAL_PROGRESS, false);
        generateProgress(assetWriter, HORIZONTAL_PROGRESS, true);

        var fontBase = new JsonObject();
        var providers = new JsonArray();

        {
            var spaces = new JsonObject();
            spaces.addProperty("type", "space");
            var advances = new JsonObject();
            SPACES.forEach((c, i) -> advances.addProperty(Character.toString(c), i));
            spaces.add("advances", advances);
            providers.add(spaces);
        }


        TEXTURES.forEach((character, id) -> {
            var bitmap = new JsonObject();
            bitmap.addProperty("type", "bitmap");
            bitmap.addProperty("file", id.toString() + ".png");
            bitmap.addProperty("ascent", 13);
            bitmap.addProperty("height", 256);
            var chars = new JsonArray();
            chars.add(Character.toString(character));
            bitmap.add("chars", chars);
            providers.add(bitmap);
        });

        fontBase.add("providers", providers);

        assetWriter.accept("assets/polyfactory/font/gui.json", fontBase.toString().getBytes(StandardCharsets.UTF_8));
    }

    private record TextBuilders(Text base) implements Function<Text, Text> {
        @Override
        public Text apply(Text text) {
            return Text.empty().append(base).append(text);
        }
    }

    public record SlicedTexture(String path, int start, int stop, boolean reverse) {};
}
