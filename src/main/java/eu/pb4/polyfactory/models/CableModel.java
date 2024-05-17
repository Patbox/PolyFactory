package eu.pb4.polyfactory.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.data.AbstractCableBlock;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Direction;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.BiConsumer;

public class CableModel {
    public static final int SIZE = (int) Math.pow(2, 6);

    public static final ItemStack[] COLORED_MODELS_BY_ID = new ItemStack[SIZE];
    //public static final ItemStack[] MODELS_BY_ID = new ItemStack[SIZE];

    private static final Item[] MODEL_ITEMS = new Item[]{
            Items.LEATHER_HELMET,
            Items.LEATHER_CHESTPLATE,
            Items.LEATHER_LEGGINGS,
            Items.LEATHER_BOOTS,
            Items.LEATHER_HORSE_ARMOR
    };
    private static int currentItemIndex;

    public static void registerAssetsEvents() {
        for (var i = 0; i < SIZE; i++) {
            //MODELS_BY_ID[i] = BaseItemProvider.requestModel(FactoryUtil.id("block/gen/cable_base/" + i));

            {
                var model = PolymerResourcePackUtils.requestModel(MODEL_ITEMS[currentItemIndex++ % MODEL_ITEMS.length], FactoryUtil.id("block/gen/cable_colored/" + i));
                var stack = new ItemStack(model.item());
                stack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(model.value()));
                COLORED_MODELS_BY_ID[i] = stack;
            }
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
                if (AbstractCableBlock.hasDirection(i, dir)) {
                    dirCount++;
                }
            }

            var base = new JsonObject();
            base.asMap().putAll(model.asMap());
            var elements = new JsonArray();

            for (var element : model.getAsJsonArray("elements")) {
                var name = element.getAsJsonObject().get("name");

                if (name == null || AbstractCableBlock.hasDirection(i, Direction.byName(name.getAsString()))
                        || (name.getAsString().equals("center") && dirCount != 1) || name.getAsString().equals("centerx")) {
                    elements.add(element);
                }
            }
            base.add("elements", elements);

            //dataWriter.accept("assets/polyfactory/models/block/gen/cable_base/" + i + ".json", base.toString().getBytes(StandardCharsets.UTF_8));

            dataWriter.accept("assets/polyfactory/models/block/gen/cable_colored/" + i + ".json", base.toString()
                    .replace("polyfactory:block/cable/cable_base", "polyfactory:block/cable/cable_colored").getBytes(StandardCharsets.UTF_8));
        }
    }
}
