package eu.pb4.polyfactory.util;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;

public class PolyFactoryConfig {
    @SerializedName("use_fast_stete_limited_blocks")
    public boolean useFastFullBlocks = true;

    private static PolyFactoryConfig instance = null;

    public static PolyFactoryConfig get() {
        if (instance == null) {
            loadConfig();
        }
        return instance;
    }

    private static void loadConfig() {
        var gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        var path = FabricLoader.getInstance().getConfigDir().resolve("polyfactory.json");
        if (Files.exists(path)) {
            try {
                instance = gson.fromJson(Files.readString(path), PolyFactoryConfig.class);
            } catch (Throwable e) {
                instance = new PolyFactoryConfig();
            }
        } else {
            instance = new PolyFactoryConfig();
        }

        try {
            Files.writeString(path, gson.toJson(instance));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
