package eu.pb4.polyfactory.models;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import net.fabricmc.loader.api.FabricLoader;
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

public class ConveyorModel {
    public static final int FRAMES = 20;
    public static final ItemStack[] ANIMATION_REGULAR = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_10 = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_01 = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_00 = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_STICKY = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_STICKY_10 = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_STICKY_01 = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_STICKY_00 = new ItemStack[1 + FRAMES];
    public static final ItemStack[][] ANIMATION_REGULAR_STICKY_X = new ItemStack[][] { ANIMATION_REGULAR_STICKY, ANIMATION_REGULAR_STICKY_10, ANIMATION_REGULAR_STICKY_01, ANIMATION_REGULAR_STICKY_00 } ;
    public static final ItemStack[][] ANIMATION_REGULAR_X = new ItemStack[][] { ANIMATION_REGULAR, ANIMATION_REGULAR_10, ANIMATION_REGULAR_01, ANIMATION_REGULAR_00 } ;
    public static final ItemStack[] ANIMATION_UP = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_UP_STICKY = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_DOWN = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_DOWN_STICKY = new ItemStack[1 + FRAMES];


    private static final String MODEL_JSON = """
                {
                  "parent": "polyfactory:block/|PREFIX|conveyor|TYPE|",
                  "textures": {
                    "1": "polyfactory:block/gen/|PREFIX|conveyor_top_|ID|"
                  }
                }
                """;
    private static final String MODEL_JSON_UP = """
                {
                  "parent": "polyfactory:block/|PREFIX|conveyor_up",
                  "textures": {
                    "1": "polyfactory:block/gen/|PREFIX|conveyor_top_|ID|"
                  }
                }
                """;

    private static final String MODEL_JSON_DOWN = """
                {
                  "parent": "polyfactory:block/|PREFIX|conveyor_down",
                  "textures": {
                    "1": "polyfactory:block/gen/|PREFIX|conveyor_top_|ID|"
                  }
                }
                """;

    private static void createItemModel(ItemStack[] array, String path, int i) {
        var model = PolymerResourcePackUtils.requestModel(Items.PURPLE_CANDLE, FactoryUtil.id(path + (i == 0 ? "" : ("_" + i))));
        var stack = new ItemStack(Items.PURPLE_CANDLE);
        stack.getOrCreateNbt().putInt("CustomModelData", model.value());
        array[i == 0 ? 0 : (array.length - i)] = stack;
    }

    public static void registerAssetsEvents() {
        for (int i = 0; i <= FRAMES; i++) {
            createItemModel(ANIMATION_REGULAR, "block/conveyor", i);
            createItemModel(ANIMATION_REGULAR_00, "block/conveyor_00", i);
            createItemModel(ANIMATION_REGULAR_01, "block/conveyor_01", i);
            createItemModel(ANIMATION_REGULAR_10, "block/conveyor_10", i);
            createItemModel(ANIMATION_UP, "block/conveyor_up", i);
            createItemModel(ANIMATION_DOWN, "block/conveyor_down", i);
            createItemModel(ANIMATION_REGULAR_STICKY, "block/sticky_conveyor", i);
            createItemModel(ANIMATION_REGULAR_STICKY_00, "block/sticky_conveyor_00", i);
            createItemModel(ANIMATION_REGULAR_STICKY_01, "block/sticky_conveyor_01", i);
            createItemModel(ANIMATION_REGULAR_STICKY_10, "block/sticky_conveyor_10", i);
            createItemModel(ANIMATION_UP_STICKY, "block/sticky_conveyor_up", i);
            createItemModel(ANIMATION_DOWN_STICKY, "block/sticky_conveyor_down", i);
        }

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(ConveyorModel::generateModels);
    }

    private static void generateModels(ResourcePackBuilder resourcePackBuilder) {
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
        //JsonObject baseConveyorModel = null;
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

            /*path = basePath.resolve("assets/polyfactory/models/block/conveyor.json");
            if (Files.exists(path)) {
                try {
                    textureTopSticky = createMovingTexture(path);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }*/

            if (textureTop.length != 0 && textureTopSticky.length != 0/* && baseConveyorModel != null*/) {
                break;
            }
        }


        for (int i = 1; i <= FRAMES; i++) {
            byte[] bytes = animJson.replace("|SPEED|", "" + i).getBytes(StandardCharsets.UTF_8);
            createVariations(resourcePackBuilder, i, bytes, textureTop, "");
            createVariations(resourcePackBuilder, i, bytes, textureTopSticky, "sticky_");
        }
    }

    private static void createVariations(ResourcePackBuilder resourcePackBuilder, int i, byte[] mcmeta, byte[] texture, String prefix) {
        resourcePackBuilder.addData("assets/polyfactory/textures/block/gen/" + prefix + "conveyor_top_" + i + ".png.mcmeta", mcmeta);
        resourcePackBuilder.addData("assets/polyfactory/textures/block/gen/" + prefix + "conveyor_top_" + i + ".png", texture);
        resourcePackBuilder.addData("assets/polyfactory/models/block/" + prefix + "conveyor_" + i + ".json", MODEL_JSON.replace("|PREFIX|", prefix).replace("|ID|", "" + i).replace("|TYPE|", "").getBytes(StandardCharsets.UTF_8));
        resourcePackBuilder.addData("assets/polyfactory/models/block/" + prefix + "conveyor_00_" + i + ".json", MODEL_JSON.replace("|PREFIX|", prefix).replace("|ID|", "" + i).replace("|TYPE|", "_00").getBytes(StandardCharsets.UTF_8));
        resourcePackBuilder.addData("assets/polyfactory/models/block/" + prefix + "conveyor_01_" + i + ".json", MODEL_JSON.replace("|PREFIX|", prefix).replace("|ID|", "" + i).replace("|TYPE|", "_01").getBytes(StandardCharsets.UTF_8));
        resourcePackBuilder.addData("assets/polyfactory/models/block/" + prefix + "conveyor_10_" + i + ".json", MODEL_JSON.replace("|PREFIX|", prefix).replace("|ID|", "" + i).replace("|TYPE|", "_10").getBytes(StandardCharsets.UTF_8));
        resourcePackBuilder.addData("assets/polyfactory/models/block/" + prefix + "conveyor_up_" + i + ".json", MODEL_JSON_UP.replace("|PREFIX|", prefix).replace("|ID|", "" + i).getBytes(StandardCharsets.UTF_8));
        resourcePackBuilder.addData("assets/polyfactory/models/block/" + prefix + "conveyor_down_" + i + ".json", MODEL_JSON_DOWN.replace("|PREFIX|", prefix).replace("|ID|", "" + i).getBytes(StandardCharsets.UTF_8));

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
