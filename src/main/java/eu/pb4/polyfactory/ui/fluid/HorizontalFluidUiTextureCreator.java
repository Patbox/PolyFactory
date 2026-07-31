package eu.pb4.polyfactory.ui.fluid;

import com.mojang.datafixers.util.Pair;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.util.ResourceUtils;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

public class HorizontalFluidUiTextureCreator {
    private final Set<Pair<Identifier, Identifier>> textures = new HashSet<>();
    private final int height;

    public HorizontalFluidUiTextureCreator(int textureHeight) {
        this.height = textureHeight;
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
                this.generateTextures(assetWriter, texture.getFirst(), texture.getSecond());
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void generateTextures(BiConsumer<String,byte[]> assetWriter, Identifier id, Identifier texture) throws IOException {
        var image = ResourceUtils.getTexture(texture);
        var file = id.withPrefix("gen/fluids_1_h" + height + "/").withSuffix(".png");

        var scale = image.getWidth() / 16;

        var out = new BufferedImage(image.getWidth(), this.height * scale, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < out.getWidth(); x++) {
            for (int y = 0; y < out.getHeight(); y++) {
                out.setRGB(x, y, image.getRGB(x, y));
            }
        }

        var bytes = new ByteArrayOutputStream();
        ImageIO.write(out, "png", bytes);
        assetWriter.accept(AssetPaths.texture(file), bytes.toByteArray());
    }
}
