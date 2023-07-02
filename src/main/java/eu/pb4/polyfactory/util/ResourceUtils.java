package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.ModInit;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ResourceUtils {

    public static BufferedImage getTexture(Identifier identifier) {
        try {
            return ImageIO.read(getJarStream("assets/" + identifier.getNamespace() + "/textures/" + identifier.getPath() +".png"));
        } catch (IOException e) {
            e.printStackTrace();
            return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public static byte[] getJarData(String jarPath) {
        for (var basePath : FabricLoader.getInstance().getModContainer(ModInit.ID).get().getRootPaths()) {
            var path = basePath.resolve(jarPath);
            if (Files.exists(path)) {
                try {
                    return Files.readAllBytes(path);
                } catch (Throwable e) {
                }
            }
        }
        return null;
    }

    public static InputStream getJarStream(String jarPath) {
        for (var basePath : FabricLoader.getInstance().getModContainer(ModInit.ID).get().getRootPaths()) {
            var path = basePath.resolve(jarPath);
            if (Files.exists(path)) {
                try {
                    return Files.newInputStream(path);
                } catch (Throwable e) {
                }
            }
        }
        return null;
    }
}
