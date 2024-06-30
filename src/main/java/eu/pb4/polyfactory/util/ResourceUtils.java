package eu.pb4.polyfactory.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ResourceUtils {

    public static BufferedImage getTexture(Identifier identifier) {
        try {
            return ImageIO.read(getJarStream("assets/" + identifier.getNamespace() + "/textures/" + identifier.getPath() + ".png"));
        } catch (Throwable e) {
            e.printStackTrace();
            return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public static JsonObject getModel(Identifier identifier) {
        try {
            return (JsonObject) JsonParser.parseString(new String(
                    getJarData("assets/" + identifier.getNamespace() + "/models/" + identifier.getPath() +" .json"), StandardCharsets.UTF_8
            ));
        } catch (Throwable e) {
            e.printStackTrace();
            return new JsonObject();
        }
    }

    public static byte[] getJarData(String jarPath) throws IOException {
        var path = findAsset(jarPath);
        return path != null ? Files.readAllBytes(path) : null;
    }

    public static InputStream getJarStream(String jarPath) throws IOException {
        var path = findAsset(jarPath);
        return path != null ? Files.newInputStream(path) : null;
    }

    public static Path findAsset(String jarPath) {
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            for (var basePath : mod.getRootPaths()) {
                var path = basePath.resolve(jarPath);
                if (Files.exists(path)) {
                    try {
                        return path;
                    } catch (Throwable e) {}
                }
            }
        }
        var path = Objects.requireNonNull(PolymerCommonUtils.getClientJar()).resolve(jarPath);
        if (Files.exists(path)) {
            try {
                return path;
            } catch (Throwable e) {}
        }
        return null;
    }
}
