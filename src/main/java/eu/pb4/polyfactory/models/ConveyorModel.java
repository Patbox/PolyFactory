package eu.pb4.polyfactory.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.BiConsumer;

public class ConveyorModel {
    public static final int FRAMES = 20;

    public static final ItemStack[][] ANIMATION_REGULAR_STICKY = new ItemStack[16][1 + FRAMES];
    public static final ItemStack[][] ANIMATION_REGULAR = new ItemStack[16][1 + FRAMES];
    public static final ItemStack[][] ANIMATION_UP = new ItemStack[16][1 + FRAMES];
    public static final ItemStack[][] ANIMATION_UP_STICKY = new ItemStack[16][1 + FRAMES];
    public static final ItemStack[][] ANIMATION_DOWN = new ItemStack[16][1 + FRAMES];
    public static final ItemStack[][] ANIMATION_DOWN_STICKY = new ItemStack[16][1 + FRAMES];
    private static final Item[] MODEL_ITEMS = new Item[]{
            Items.WHITE_CARPET,
            Items.ORANGE_CARPET,
            Items.MAGENTA_CARPET,
            Items.LIGHT_BLUE_CARPET,
            Items.YELLOW_CARPET,
            Items.LIME_CARPET,
            Items.PINK_CARPET,
            Items.GRAY_CARPET,
            Items.LIGHT_GRAY_CARPET,
            Items.CYAN_CARPET,
            Items.PURPLE_CARPET,
            Items.BLUE_CARPET,
            Items.BROWN_CARPET,
            Items.GREEN_CARPET,
            Items.RED_CARPET,
            Items.BLACK_CARPET
    };
    private static final String MODEL_JSON = """
            {
              "parent": "polyfactory:block/conveyor|TYPE|",
              "textures": {
                "1": "polyfactory:block/gen/|PREFIX|conveyor_top_|ID|"
              }
            }
            """;
    private static final String MODEL_JSON_UP = """
            {
              "parent": "polyfactory:block/conveyor_up|TYPE|",
              "textures": {
                "1": "polyfactory:block/gen/|PREFIX|conveyor_top_|ID|"
              }
            }
            """;
    private static final String MODEL_JSON_DOWN = """
            {
              "parent": "polyfactory:block/conveyor_down|TYPE|",
              "textures": {
                "1": "polyfactory:block/gen/|PREFIX|conveyor_top_|ID|"
              }
            }
            """;
    private static int currentItemIndex;
    public static final ItemStack REGULAR_FAST = new ItemStack(MODEL_ITEMS[currentItemIndex++]);
    public static final ItemStack UP_FAST = new ItemStack(MODEL_ITEMS[currentItemIndex++]);
    public static final ItemStack DOWN_FAST = new ItemStack(MODEL_ITEMS[currentItemIndex++]);
    public static final ItemStack STICKY_REGULAR_FAST = new ItemStack(MODEL_ITEMS[currentItemIndex++]);
    public static final ItemStack STICKY_UP_FAST = new ItemStack(MODEL_ITEMS[currentItemIndex++]);
    public static final ItemStack STICKY_DOWN_FAST = new ItemStack(MODEL_ITEMS[currentItemIndex++]);

    private static void createItemModel(ItemStack[] array, String path, int i) {
        var model = PolymerResourcePackUtils.requestModel(MODEL_ITEMS[currentItemIndex++ % MODEL_ITEMS.length], FactoryUtil.id(path + (i == 0 ? "" : ("/" + i))));
        var stack = new ItemStack(model.item());
        stack.getOrCreateNbt().putInt("CustomModelData", model.value());
        array[i == 0 ? 0 : (array.length - i)] = stack;
    }

    public static void registerAssetsEvents() {
        createFast(REGULAR_FAST, "", "");
        createFast(UP_FAST, "", "_up");
        createFast(DOWN_FAST, "", "_down");
        createFast(STICKY_REGULAR_FAST, "sticky_", "");
        createFast(STICKY_UP_FAST, "sticky_", "_up");
        createFast(STICKY_DOWN_FAST, "sticky_", "_down");

        for (int i = 0; i <= FRAMES; i++) {
            for (var a = 0; a < 16; a++) {
                String addition = a == 0 ? "" : ("_" + a);

                createItemModel(ANIMATION_REGULAR[a], "block/conveyor" + addition, i);
                createItemModel(ANIMATION_UP[a], "block/conveyor_up" + addition, i);
                createItemModel(ANIMATION_DOWN[a], "block/conveyor_down" + addition, i);
                createItemModel(ANIMATION_REGULAR_STICKY[a], "block/sticky_conveyor" + addition, i);
                createItemModel(ANIMATION_UP_STICKY[a], "block/sticky_conveyor_up" + addition, i);
                createItemModel(ANIMATION_DOWN_STICKY[a], "block/sticky_conveyor_down" + addition, i);
            }
        }

        if (ModInit.DYNAMIC_ASSETS) {
            PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register((resourcePackBuilder) -> ConveyorModel.generateModels(resourcePackBuilder::addData));
        }
    }

    private static void createFast(ItemStack stack, String prefix, String suffix) {
        var path = FactoryUtil.id("block/" + prefix + "conveyor" + suffix + "_fast");
        stack.getOrCreateNbt().putInt("CustomModelData", PolymerResourcePackUtils.requestModel(stack.getItem(), path).value());
    }

    public static void generateModels(BiConsumer<String, byte[]> dataWriter) {
        var animJson = """
                {
                  "animation": {
                    "interpolate": false,
                    "frametime": |SPEED|
                  }
                }
                """;
        byte[] textureTop = new byte[0];
        byte[] textureTopSticky = new byte[0];
        var models = new HashMap<String, JsonObject>();
        //JsonObject upConveyorModel = null;
        //JsonObject downConveyorModel = null;

        for (var basePath : FabricLoader.getInstance().getModContainer(ModInit.ID).get().getRootPaths()) {
            var path = basePath.resolve("assets/polyfactory/textures/block/conveyor_top.png");
            if (Files.exists(path)) {
                try {
                    textureTop = createMovingTexture(path);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            path = path.getParent().resolve("sticky_conveyor_top.png");
            if (Files.exists(path)) {
                try {
                    textureTopSticky = createMovingTexture(path);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            for (var variant : new String[]{"conveyor", "conveyor_up", "conveyor_down"}) {
                path = basePath.resolve("assets/polyfactory/models/block/" + variant + ".json");
                if (Files.exists(path)) {
                    try {
                        models.put(variant, (JsonObject) JsonParser.parseReader(Files.newBufferedReader(path)));
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }

            if (textureTop.length != 0 && textureTopSticky.length != 0 && models.size() == 3) {
                break;
            }
        }

        for (var variant : new String[]{"conveyor", "conveyor_up", "conveyor_down"}) {
            var model = models.get(variant);
            for (int i = 1; i < 16; i++) {
                var base = new JsonObject();
                base.asMap().putAll(model.asMap());
                var elements = new JsonArray();

                for (var element : model.getAsJsonArray("elements")) {
                    var name = element.getAsJsonObject().get("name");

                    if (name == null || !(switch (name.getAsString()) {
                        case "top" -> ConveyorBlock.hasTop(i);
                        case "bottom" -> ConveyorBlock.hasBottom(i);
                        case "front" -> ConveyorBlock.hasNext(i);
                        case "back" -> ConveyorBlock.hasPrevious(i);
                        case "top_front" -> ConveyorBlock.hasTop(i) && ConveyorBlock.hasNext(i);
                        case "bottom_front" -> ConveyorBlock.hasBottom(i) && ConveyorBlock.hasNext(i);
                        case "top_back" -> ConveyorBlock.hasTop(i) && ConveyorBlock.hasPrevious(i);
                        case "bottom_back" -> ConveyorBlock.hasBottom(i) && ConveyorBlock.hasPrevious(i);
                        default -> false;
                    })) {
                        elements.add(element);
                    }
                }
                base.add("elements", elements);

                dataWriter.accept("assets/polyfactory/models/block/" + variant + "_" + i + ".json", base.toString().getBytes(StandardCharsets.UTF_8));
                dataWriter.accept("assets/polyfactory/models/block/sticky_" + variant + "_" + i + ".json", ("""
                        {
                          "parent": "polyfactory:block/""" + variant + "_" + i
                        + """
                        ",
                         "textures": {
                          "1": "polyfactory:block/sticky_conveyor_top"
                         }
                        }
                        """).getBytes(StandardCharsets.UTF_8));
            }
        }


        for (int i = 1; i <= FRAMES; i++) {
            byte[] bytes = animJson.replace("|SPEED|", "" + i).getBytes(StandardCharsets.UTF_8);
            createVariations(dataWriter, i, bytes, textureTop, "");
            createVariations(dataWriter, i, bytes, textureTopSticky, "sticky_");
        }
    }

    private static void createVariations(BiConsumer<String, byte[]> dataWriter, int i, byte[] mcmeta, byte[] texture, String prefix) {
        dataWriter.accept("assets/polyfactory/textures/block/gen/" + prefix + "conveyor_top_" + i + ".png.mcmeta", mcmeta);
        dataWriter.accept("assets/polyfactory/textures/block/gen/" + prefix + "conveyor_top_" + i + ".png", texture);

        for (int a = 0; a < 16; a++) {
            var base = (a == 0 ? "" : ("_" + a + ""));
            var addition = (a == 0 ? "/" : ("_" + a + "/")) + i;

            dataWriter.accept("assets/polyfactory/models/block/" + prefix + "conveyor" + addition + ".json", MODEL_JSON.replace("|PREFIX|", prefix).replace("|ID|", "" + i).replace("|TYPE|", base).getBytes(StandardCharsets.UTF_8));
            dataWriter.accept("assets/polyfactory/models/block/" + prefix + "conveyor_up" + addition + ".json", MODEL_JSON_UP.replace("|PREFIX|", prefix).replace("|ID|", "" + i).replace("|TYPE|", base).getBytes(StandardCharsets.UTF_8));
            dataWriter.accept("assets/polyfactory/models/block/" + prefix + "conveyor_down" + addition + ".json", MODEL_JSON_DOWN.replace("|PREFIX|", prefix).replace("|ID|", "" + i).replace("|TYPE|", base).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static byte[] createMovingTexture(Path path) throws IOException {
        var image = ImageIO.read(Files.newInputStream(path));
        var imageSize = 32;
        var textureXSize = 16;
        var textureYSize = 18;

        BufferedImage output;
        if (image.getColorModel() instanceof IndexColorModel colorModel) {
            output = new BufferedImage(imageSize, imageSize * imageSize, image.getType(), colorModel);
        } else {
            output = new BufferedImage(imageSize, imageSize * imageSize, image.getType());
        }

        var scale = imageSize / image.getWidth();

        for (var i = 0; i < imageSize; i++) {
            var position = i * imageSize;

            for (var x = 0; x < textureXSize; x++) {
                for (var y = 0; y < textureYSize; y++) {
                    var pixel = image.getRGB(((x + i) % textureXSize) / scale, y / scale);

                    output.setRGB(x, y + position, pixel);
                    output.setRGB(x + 16, y + position, pixel);
                }
            }
        }

        var out = new ByteArrayOutputStream();
        ImageIO.write(output, "png", out);
        return out.toByteArray();
    }
}
