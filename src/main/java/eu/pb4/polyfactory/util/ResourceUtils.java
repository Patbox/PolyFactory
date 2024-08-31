package eu.pb4.polyfactory.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.pb4.polyfactory.ModInit;
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
            ModInit.LOGGER.error("Failed to load texture '" + identifier + "'", e);
            return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public static JsonObject getModel(Identifier identifier) {
        try {
            return (JsonObject) JsonParser.parseString(new String(
                    Objects.requireNonNull(getJarData("assets/" + identifier.getNamespace() + "/models/" + identifier.getPath() + ".json")), StandardCharsets.UTF_8
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
                    return path;
                }
            }
        }
        var path = Objects.requireNonNull(PolymerCommonUtils.getClientJarRoot()).resolve(jarPath);
        if (Files.exists(path)) {
            return path;
        }
        return null;
    }

    public static JsonObject getElementResolvedModel(Identifier baseModel) {
        var model = getModel(baseModel);
        if (!model.has("elements") && model.has("parent")) {
            var parent = getElementResolvedModel(Identifier.of(model.get("parent").getAsString()));
            for (var key : model.keySet()) {
                if (parent.has(key) && parent.get(key).isJsonObject()) {
                    var out = parent.get(key).getAsJsonObject();
                    var in = model.get(key).getAsJsonObject();
                    for (var key2 : in.keySet()) {
                        out.add(key2, in.get(key2));
                    }
                } else if (!key.equals("parent")) {
                    parent.add(key, model.get(key));
                }
            }
            return parent;
        }

        return model;
    }
}
