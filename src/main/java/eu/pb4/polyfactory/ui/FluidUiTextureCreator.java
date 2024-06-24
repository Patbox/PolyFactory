package eu.pb4.polyfactory.ui;

import eu.pb4.polyfactory.FactoryRegistries;
import eu.pb4.polyfactory.util.ResourceUtils;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

public class FluidUiTextureCreator {
    private final Set<Identifier> textures = new HashSet<>();
    private final int width;
    private final int textureHeight;

    public FluidUiTextureCreator(int textureWidth) {
        this.width = textureWidth;
        this.textureHeight = 18;
    }

    public void registerTextures(Identifier identifier) {
        this.textures.add(identifier);
    }
    public void setup() {
        for (var fluid : FactoryRegistries.FLUID_TYPES.getIds()) {
            this.registerTextures(fluid);
        }
        RegistryEntryAddedCallback.event(FactoryRegistries.FLUID_TYPES).register((rawId, id, object) -> {
            this.registerTextures(id);
        });
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register((b) -> this.generateAssets(b::addData));
    }

    public void generateAssets(BiConsumer<String, byte[]> assetWriter) {
        try {
            for (var texture : textures) {
                this.generateSplitTextures(assetWriter, texture);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void generateSplitTextures(BiConsumer<String,byte[]> assetWriter, Identifier texture) throws IOException {
        var image = ResourceUtils.getTexture(texture.withPrefixedPath("pf_fluids/"));
        var file = texture.withPrefixedPath("gen/fluids_" + width + "/").withSuffixedPath("/");

        var scale = image.getHeight() / 32;

        for (int i = 0; i < textureHeight; i++) {
            var out = new BufferedImage(this.width * scale, scale, image.getType());
            for (int x = 0; x < out.getWidth(); x++) {
                for (int y = 0; y < out.getHeight(); y++) {
                    out.setRGB(x, y, image.getRGB(x, y + i * scale));
                }
            }

            var bytes = new ByteArrayOutputStream();

            ImageIO.write(out, "png", bytes);

            assetWriter.accept(AssetPaths.texture(file.withSuffixedPath(i + ".png")), bytes.toByteArray());
        }
    }
}
