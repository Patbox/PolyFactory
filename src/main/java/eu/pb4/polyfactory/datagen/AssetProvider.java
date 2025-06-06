package eu.pb4.polyfactory.datagen;

import com.google.common.hash.HashCode;
import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import eu.pb4.polyfactory.item.FactoryDebugItems;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.tool.SpoutMolds;
import eu.pb4.polyfactory.models.ConveyorModels;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.ResourceUtils;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.*;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.bool.CustomModelDataFlagProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.bool.UsingItemProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.numeric.CustomModelDataFloatProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.select.DisplayContextProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.tint.ConstantTintSource;
import eu.pb4.polymer.resourcepack.extras.api.format.item.tint.CustomModelDataTintSource;
import eu.pb4.polymer.resourcepack.extras.api.format.item.tint.PotionTintSource;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelAsset;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.ColorHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static eu.pb4.polyfactory.ModInit.id;

class AssetProvider implements DataProvider {
    private final DataOutput output;

    public AssetProvider(FabricDataOutput output) {
        this.output = output;
    }

    public static void runWriters(BiConsumer<String, byte[]> assetWriter) {
        ConveyorModels.generateModels(assetWriter);
        UiResourceCreator.generateAssets(assetWriter);
        FactoryModels.COLORED_CABLE.generateModels(assetWriter);
        FactoryModels.COLORED_WALL_CABLE.generateModels(assetWriter);
        FactoryModels.PIPE.generateModels(assetWriter);
        FactoryModels.BLOCK_FLUID_TANK.generateModels(assetWriter);
        var map = new HashMap<Identifier, ItemAsset>();
        createItems(map::put);
        map.forEach((id, asset) -> assetWriter.accept(AssetPaths.itemAsset(id), asset.toJson().getBytes(StandardCharsets.UTF_8)));
        createNumberButtons(assetWriter);

        var moldTexture = new MoldTextures();

        for (var mold : FactoryItems.MOLDS) {
            createMold(assetWriter, mold, moldTexture);
        }
    }
    private static class MoldTextures {
        BufferedImage steelBase = ResourceUtils.getTexture(id("item/mold/template/template"));
        BufferedImage steelBorder = ResourceUtils.getTexture(id("item/mold/template/template_border"));
        BufferedImage clayBase = ResourceUtils.getTexture(id("item/mold/template/template_clay"));
        BufferedImage clayBorder = ResourceUtils.getTexture(id("item/mold/template/template_clay_border"));
        BufferedImage hardenedBase = ResourceUtils.getTexture(id("item/mold/template/template_hardened"));
        BufferedImage hardenedBorder = ResourceUtils.getTexture(id("item/mold/template/template_hardened_border"));
    }

    private static void createMold(BiConsumer<String, byte[]> assetWriter, SpoutMolds mold, MoldTextures moldTexture) {
        var stencil = ResourceUtils.getTexture(mold.name().withPrefixedPath("item/mold/source/"));


        {
            var id = Registries.ITEM.getId(mold.mold());
            assetWriter.accept(AssetPaths.itemModel(id),
                    ModelAsset.builder().parent(Identifier.of("item/generated"))
                            .texture("layer0", id.withPrefixedPath("item/").toString())
                            .build().toBytes()
            );
            createStencilTexture(id.withPrefixedPath("item/"), moldTexture.steelBase, moldTexture.steelBorder, stencil, assetWriter);
        }

        {
            var id = Registries.ITEM.getId(mold.clay());
            assetWriter.accept(AssetPaths.itemModel(id),
                    ModelAsset.builder().parent(Identifier.of("item/generated"))
                            .texture("layer0", id.withPrefixedPath("item/").toString())
                            .build().toBytes()
            );
            createStencilTexture(id.withPrefixedPath("item/"), moldTexture.clayBase, moldTexture.clayBorder, stencil, assetWriter);

        }


        {
            var id = Registries.ITEM.getId(mold.hardened());
            assetWriter.accept(AssetPaths.itemModel(id),
                    ModelAsset.builder().parent(Identifier.of("item/generated"))
                            .texture("layer0", id.withPrefixedPath("item/").toString())
                            .build().toBytes()
            );
            createStencilTexture(id.withPrefixedPath("item/"), moldTexture.hardenedBase, moldTexture.hardenedBorder, stencil, assetWriter);
        }
    }

    private static void createStencilTexture(Identifier identifier, BufferedImage base, BufferedImage border, BufferedImage stencil, BiConsumer<String,byte[]> assetWriter) {
        if (base.getWidth() != border.getWidth() || border.getWidth() != stencil.getWidth() || base.getHeight() != border.getHeight() || border.getHeight() != stencil.getHeight()) {
            throw new IllegalArgumentException("Mismatched image width and height for stenccil texture " + identifier);
        }

        var image = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (var x = 0; x < base.getWidth(); x++) {
            for (var y = 0; y < base.getHeight(); y++) {
                var s = stencil.getRGB(x, y);
                if (ColorHelper.getAlpha(s) == 0) {
                    image.setRGB(x, y, base.getRGB(x, y));
                } else if (s == 0xFF000000) {
                    image.setRGB(x, y, border.getRGB(x, y));
                }
            }
        }
        var stream = new ByteArrayOutputStream();

        try {
            ImageIO.write(image, "png", stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        assetWriter.accept(AssetPaths.texture(identifier.withSuffixedPath(".png")), stream.toByteArray());
    }

    private static void createItems(BiConsumer<Identifier, ItemAsset> consumer) {
        var fromItem = new BiConsumer<Item, Function<Identifier, ItemAsset>>() {
            @Override
            public void accept(Item item, Function<Identifier, ItemAsset> function) {
                var id = Registries.ITEM.getId(item);
                consumer.accept(id, function.apply(id));
            }
        };
        for (var item : Registries.ITEM) {
            var id = Registries.ITEM.getId(item);
            if (!id.getNamespace().equals("polyfactory")) {
                continue;
            }
            consumer.accept(id, new ItemAsset(new BasicItemModel(id.withPrefixedPath(item instanceof BlockItem ? "block/" : "item/")), ItemAsset.Properties.DEFAULT));
        }
        fromItem.accept(FactoryItems.WINDMILL_SAIL, id -> new ItemAsset(new BasicItemModel(id.withPrefixedPath("item/"),
                List.of(new ConstantTintSource(-1), new CustomModelDataTintSource(0, 0xFFFFFF))), ItemAsset.Properties.DEFAULT));

        fromItem.accept(FactoryItems.CABLE, id -> new ItemAsset(new BasicItemModel(id.withPrefixedPath("item/"),
                List.of(new ConstantTintSource(-1), new CustomModelDataTintSource(0, 0xFFFFFF))), ItemAsset.Properties.DEFAULT));

        fromItem.accept(FactoryItems.ARTIFICIAL_DYE, id -> new ItemAsset(new BasicItemModel(id.withPrefixedPath("item/"),
                List.of(new CustomModelDataTintSource(0, -1))), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.BRITTLE_POTION, id -> new ItemAsset(new BasicItemModel(id.withPrefixedPath("item/"),
                List.of(new PotionTintSource())), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.PORTABLE_REDSTONE_TRANSMITTER, id -> new ItemAsset(new BasicItemModel(id.withPrefixedPath("item/"),
                List.of(new CustomModelDataTintSource(0, 0xFFFFFF))), ItemAsset.Properties.DEFAULT));

        fromItem.accept(FactoryItems.SPRAY_CAN, id -> new ItemAsset(new ConditionItemModel(new CustomModelDataFlagProperty(0),
                new BasicItemModel(id.withPrefixedPath("item/"), List.of(new CustomModelDataTintSource(0, -1))),
                new BasicItemModel(id.withPrefixedPath("item/").withSuffixedPath("_empty"), List.of())),
                ItemAsset.Properties.DEFAULT));

        fromItem.accept(FactoryItems.LAMP, id -> new ItemAsset(new BasicItemModel(id("block/colored_lamp_off"),
                List.of(new ConstantTintSource(-1), new CustomModelDataTintSource(0, 0xFFFFFF))), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.INVERTED_LAMP, id -> new ItemAsset(new BasicItemModel(id("block/colored_lamp_on"),
                List.of(new ConstantTintSource(-1), new CustomModelDataTintSource(0, 0xFFFFFF))), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.CAGED_LAMP, id -> new ItemAsset(new BasicItemModel(id("block/caged_lamp_off"),
                List.of(new ConstantTintSource(-1), new CustomModelDataTintSource(0, 0xFFFFFF))), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.INVERTED_CAGED_LAMP, id -> new ItemAsset(new BasicItemModel(id("block/caged_lamp_on"),
                List.of(new ConstantTintSource(-1), new CustomModelDataTintSource(0, 0xFFFFFF))), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.FIXTURE_LAMP, id -> new ItemAsset(new BasicItemModel(id("block/fixture_lamp_off"),
                List.of(new ConstantTintSource(-1), new CustomModelDataTintSource(0, 0xFFFFFF))), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.INVERTED_FIXTURE_LAMP, id -> new ItemAsset(new BasicItemModel(id("block/fixture_lamp_on"),
                List.of(new ConstantTintSource(-1), new CustomModelDataTintSource(0, 0xFFFFFF))), ItemAsset.Properties.DEFAULT));

        fromItem.accept(FactoryItems.FAN, id -> new ItemAsset(new BasicItemModel(id.withPrefixedPath("block/").withSuffixedPath("_base")), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.FUNNEL, id -> new ItemAsset(new BasicItemModel(id.withPrefixedPath("block/").withSuffixedPath("_out")), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.SLOT_AWARE_FUNNEL, id -> new ItemAsset(new BasicItemModel(id.withPrefixedPath("block/").withSuffixedPath("_out")), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.FLUID_TANK, id -> new ItemAsset(new BasicItemModel(id.withPrefixedPath("block/").withSuffixedPath("/single_single_single")), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.INVERTED_REDSTONE_LAMP, id -> new ItemAsset(new BasicItemModel(Identifier.ofVanilla("block/redstone_lamp_on")), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.ELECTRIC_MOTOR, id -> new ItemAsset(new BasicItemModel(id("block/motor")), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.DATA_MEMORY, id -> new ItemAsset(new BasicItemModel(id.withPrefixedPath("item/")), ItemAsset.Properties.DEFAULT));
        fromItem.accept(FactoryItems.EJECTOR, id -> new ItemAsset(new BasicItemModel(id.withPrefixedPath("item/")), ItemAsset.Properties.DEFAULT));

        fromItem.accept(FactoryItems.PRESSURE_FLUID_GUN, id -> new ItemAsset(new ConditionItemModel(
                new UsingItemProperty(),
                new BasicItemModel(id.withPrefixedPath("item/").withSuffixedPath("_active")),
                new BasicItemModel(id.withPrefixedPath("item/"))
        ), new ItemAsset.Properties(false)));

        for (var item : List.of(FactoryItems.TINY_POTATO_SPRING, FactoryItems.GOLDEN_TINY_POTATO_SPRING, FactoryItems.PIPE, FactoryItems.MECHANICAL_DRAIN, FactoryItems.PORTABLE_FLUID_TANK,
                FactoryItems.LARGE_STEEL_GEAR,
                FactoryItems.PUMP, FactoryItems.STEEL_BUTTON, FactoryDebugItems.ROTATION_DEBUG)) {
            fromItem.accept(item, id -> new ItemAsset(new BasicItemModel(id.withPrefixedPath("item/")), ItemAsset.Properties.DEFAULT));
        }

        fromItem.accept(FactoryItems.STEEL_GEAR, id -> new ItemAsset(SelectItemModel.builder(new DisplayContextProperty())
                .withCase(List.of(
                        ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                        ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                        ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                        ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                ), new BasicItemModel(id.withPrefixedPath("item/").withSuffixedPath("_world")))
                .fallback(new BasicItemModel(id.withPrefixedPath("item/")))
                .build(), ItemAsset.Properties.DEFAULT));

        consumer.accept(id("debug_item"), new ItemAsset(
                new BasicItemModel(id("item/debug_item"), List.of(new CustomModelDataTintSource(0, 0xFFFFFF))), ItemAsset.Properties.DEFAULT)
        );

        consumer.accept(id("-/block/windmill_sail"), new ItemAsset(
                new BasicItemModel(id("block/windmill_sail"), List.of(new CustomModelDataTintSource(0, 0xFFFFFF))), ItemAsset.Properties.DEFAULT));
        consumer.accept(id("-/block/windmill_sail_flip"), new ItemAsset(
                new BasicItemModel(id("block/windmill_sail_flip"), List.of(new CustomModelDataTintSource(0, 0xFFFFFF))), ItemAsset.Properties.DEFAULT));

        consumer.accept(id("-/block/redstone_input_overlay"), new ItemAsset(
                new BasicItemModel(id("block/redstone_input_overlay"), List.of(new CustomModelDataTintSource(0, 0xFF0000))), ItemAsset.Properties.DEFAULT));
        consumer.accept(id("-/block/redstone_output_overlay"), new ItemAsset(
                new BasicItemModel(id("block/redstone_output_overlay"), List.of(new CustomModelDataTintSource(0, 0xFF0000))), ItemAsset.Properties.DEFAULT));

        consumer.accept(id("placeholder"), new ItemAsset(new BasicItemModel(id("item/placeholder")), ItemAsset.Properties.DEFAULT));

        var list = new ArrayList<ItemModel>();

        {
            var offsets = new int[]{-18 * 3, -18 * 2, -18};

            for (int i = 0; i < 3; i++) {
                var builder = RangeDispatchItemModel.builder(new CustomModelDataFloatProperty(i)).scale(15);
                builder.fallback(new EmptyItemModel());

                for (int a = 1; a <= 14; a++) {
                    builder.entry(a, new BasicItemModel(id("sgui/elements/gen/generic_bar_" + a + "_offset_" + offsets[i]),
                            List.of(new CustomModelDataTintSource(i, 0xFFFFFF))));
                }

                list.add(new ConditionItemModel(new CustomModelDataFlagProperty(i),
                        new CompositeItemModel(List.of(
                                new BasicItemModel(id("sgui/elements/generic_bar_background_offset_" + offsets[i])),
                                builder.build())
                        ),
                        new EmptyItemModel()
                ));
            }
        }

        consumer.accept(GuiTextures.LEFT_SHIFTED_3_BARS.get(DataComponentTypes.ITEM_MODEL), new ItemAsset(new CompositeItemModel(list), new ItemAsset.Properties(false, true)));
    }


    private static void createNumberButtons(BiConsumer<String,byte[]> assetWriter) {
        var empty = ResourceUtils.getTexture(id("sgui/elements/numbered_buttons/empty"));
        for (int i = 0; i < 100; i++) {
            var out = new BufferedImage(empty.getWidth(), empty.getHeight(), empty.getType());
            for (int x = 0; x < out.getWidth(); x++) {
                for (int y = 0; y < out.getHeight(); y++) {
                    out.setRGB(x, y, empty.getRGB(x, y));
                }
            }
            var proxy = new DrawableCanvas() {
                @Override
                public byte getRaw(int x, int y) {
                    if (x < 0 || y < 0 || x >= this.getWidth() || y >= this.getHeight()) {
                        return 0;
                    }
                    return CanvasUtils.findClosestRawColorARGB(out.getRGB(x, y));
                }

                @Override
                public void setRaw(int x, int y, byte b) {
                    if (x < 0 || y < 0 || x >= this.getWidth() || y >= this.getHeight()) {
                        return;
                    }
                    var color = CanvasColor.getFromRaw(b);
                    int rgb = 0;
                    if (color == CanvasColor.WHITE_HIGH) {
                        rgb = 0xFFFFFFFF;
                    } else if (color == CanvasColor.WHITE_LOW) {
                        rgb = 0xFF222222;
                    }

                    out.setRGB(x, y, rgb);
                }

                @Override
                public int getHeight() {
                    return out.getHeight();
                }

                @Override
                public int getWidth() {
                    return out.getWidth();
                }
            };


            var text = String.valueOf(i);
            var width = DefaultFonts.VANILLA.getTextWidth(text, 8);
            //DefaultFonts.VANILLA.drawText(proxy, text, proxy.getWidth() / 2 - width / 2, 5, 8, CanvasColor.WHITE_LOW);
            DefaultFonts.VANILLA.drawText(proxy, text, proxy.getWidth() / 2 - width / 2 - 1, 4, 8, CanvasColor.WHITE_HIGH);

            var buf = new ByteArrayOutputStream();

            try {
                ImageIO.write(out, "png", buf);
            } catch (IOException e) {

            }

            assetWriter.accept("assets/polyfactory/textures/sgui/elements/numbered_buttons/num_"  + i + ".png", buf.toByteArray());
        }
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        BiConsumer<String, byte[]> assetWriter = (path, data) -> {
            try {
                writer.write(this.output.getPath().resolve(path), data, HashCode.fromBytes(data));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        return CompletableFuture.runAsync(() -> {
            runWriters(assetWriter);
        }, Util.getMainWorkerExecutor());
    }

    @Override
    public String getName() {
        return "polyfactory:assets";
    }
}
