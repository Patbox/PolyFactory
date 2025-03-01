package eu.pb4.polyfactory.datagen;

import com.google.common.hash.HashCode;
import eu.pb4.mapcanvas.api.core.CanvasColor;
import eu.pb4.mapcanvas.api.core.CanvasImage;
import eu.pb4.mapcanvas.api.core.DrawableCanvas;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.mapcanvas.api.utils.CanvasUtils;
import eu.pb4.polyfactory.item.FactoryDebugItems;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.ConveyorModels;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.ResourceUtils;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

class AssetProvider implements DataProvider {
    private final DataOutput output;

    public AssetProvider(FabricDataOutput output) {
        this.output = output;
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

    public static void runWriters(BiConsumer<String,byte[]> assetWriter) {
        ConveyorModels.generateModels(assetWriter);
        UiResourceCreator.generateAssets(assetWriter);
        FactoryModels.COLORED_CABLE.generateModels(assetWriter);
        FactoryModels.COLORED_WALL_CABLE.generateModels(assetWriter);
        FactoryModels.PIPE.generateModels(assetWriter);
        FactoryModels.BLOCK_FLUID_TANK.generateModels(assetWriter);
        createNumberButtons(assetWriter);
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
    public String getName() {
        return "polyfactory:assets";
    }
}
