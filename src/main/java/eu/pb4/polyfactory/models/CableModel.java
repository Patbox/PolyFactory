package eu.pb4.polyfactory.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.data.CableBlock;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;

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

public class CableModel {
    public static final int SIZE = (int) Math.pow(2, 6);

    public static final ItemStack[] MODELS_BY_ID = new ItemStack[SIZE];
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
    private static int currentItemIndex;

    public static void registerAssetsEvents() {
        for (var i = 0; i < SIZE; i++) {
            var model = PolymerResourcePackUtils.requestModel(MODEL_ITEMS[currentItemIndex++ % MODEL_ITEMS.length], FactoryUtil.id("block/gen/cable/" + i));
            var stack = new ItemStack(model.item());
            stack.getOrCreateNbt().putInt("CustomModelData", model.value());
            MODELS_BY_ID[i] = stack;
        }
        if (ModInit.DYNAMIC_ASSETS) {
            PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register((resourcePackBuilder) -> CableModel.generateModels(resourcePackBuilder::addData));
        }
    }

    public static void generateModels(BiConsumer<String, byte[]> dataWriter) {
        JsonObject model = null;

        for (var basePath : FabricLoader.getInstance().getModContainer(ModInit.ID).get().getRootPaths()) {
            var path = basePath.resolve("assets/polyfactory/models/block/cable_base.json");
            if (Files.exists(path)) {
                try {
                    model = (JsonObject) JsonParser.parseReader(Files.newBufferedReader(path));
                    break;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        for (int i = 0; i < SIZE; i++) {
            int dirCount = 0;
            for (var dir : Direction.values()) {
                if (CableBlock.hasDirection(i, dir)) {
                    dirCount++;
                }
            }

            var base = new JsonObject();
            base.asMap().putAll(model.asMap());
            var elements = new JsonArray();

            for (var element : model.getAsJsonArray("elements")) {
                var name = element.getAsJsonObject().get("name");

                if (name == null || CableBlock.hasDirection(i, Direction.byName(name.getAsString()))
                        || (name.getAsString().equals("center") && dirCount != 1) || name.getAsString().equals("centerx")) {
                    elements.add(element);
                }
            }
            base.add("elements", elements);

            dataWriter.accept("assets/polyfactory/models/block/gen/cable/" + i + ".json", base.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
