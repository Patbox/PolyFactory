package eu.pb4.polyfactory.ui;

import com.mojang.datafixers.util.Pair;
import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.util.ResourceUtils;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.resources.Identifier;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

public class FluidUiTextureCreator {
    private final Set<Pair<Identifier, Identifier>> textures = new HashSet<>();
    private final int width;
    private final int textureHeight;

    public FluidUiTextureCreator(int textureWidth) {
        this.width = textureWidth;
        this.textureHeight = 16;
    }

    public void registerTextures(Identifier id, FluidType<?> object) {
        this.textures.add(Pair.of(id, object.texture()));
    }
    public void setup() {
        for (var fluid : FactoryRegistries.FLUID_TYPES.keySet()) {
            this.registerTextures(fluid, Objects.requireNonNull(FactoryRegistries.FLUID_TYPES.getValue(fluid)));
        }
        RegistryEntryAddedCallback.event(FactoryRegistries.FLUID_TYPES).register((rawId, id, object) -> {
            this.registerTextures(id, object);
        });
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register((b) -> this.generateAssets(b::addData));
    }

    public void generateAssets(BiConsumer<String, byte[]> assetWriter) {
        try {
            for (var texture : textures) {
                this.generateSplitTextures(assetWriter, texture.getFirst(), texture.getSecond());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void generateSplitTextures(BiConsumer<String,byte[]> assetWriter, Identifier id, Identifier texture) throws IOException {
        var image = ResourceUtils.getTexture(texture);
        var file = id.withPrefix("gen/fluids_" + width + "/").withSuffix("/");

        var scale = image.getWidth() / 16;

        for (int i = 0; i < textureHeight; i++) {
            var out = new BufferedImage(this.width * scale, scale, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < out.getWidth(); x++) {
                for (int y = 0; y < out.getHeight(); y++) {
                    out.setRGB(x, y, image.getRGB(x % image.getWidth(), y + i * scale));
                }
            }

            var bytes = new ByteArrayOutputStream();

            ImageIO.write(out, "png", bytes);

            assetWriter.accept(AssetPaths.texture(file.withSuffix(i + ".png")), bytes.toByteArray());
        }
    }
}
