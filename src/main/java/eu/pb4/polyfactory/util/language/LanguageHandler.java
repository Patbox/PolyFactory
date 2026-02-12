package eu.pb4.polyfactory.util.language;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.locale.DeprecatedTranslationsInfo;
import net.minecraft.locale.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.server.translations.api.LocalizationTarget;
import xyz.nucleoid.server.translations.api.language.ServerLanguage;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public record LanguageHandler(Map<String, String> assetLanguage, ServerLanguage serverLanguage) {
    private static final LoadingCache<String, LanguageHandler> CACHE = CacheBuilder.newBuilder().maximumSize(32).expireAfterAccess(Duration.ofMinutes(5))
            .build(new CacheLoader<>() {
                @Override
                public @NotNull LanguageHandler load(@NotNull String key) throws Exception {
                    try {
                        return LanguageHandler.load(key);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        return new LanguageHandler(Map.of(), LocalizationTarget.ofSystem().getLanguage());
                    }
                }
            });


    public static LanguageHandler get(String code) {
        return CACHE.getUnchecked(code);
    }

    private static LanguageHandler load(String code) throws Throwable {
        var cached = FabricLoader.getInstance().getGameDir().resolve(".polydex/runtime_cache/" + code +".json");
        if (Files.exists(cached)) {
            var map = new HashMap<String, String>();
            Language.loadFromJson(Files.newInputStream(cached), map::put);
            return new LanguageHandler(map, ServerLanguage.getLanguage(code));
        }

        VanillaLanguageDownloader.setup();
        while (VanillaLanguageDownloader.downloading) {
            Thread.sleep(10);
        }

        var map = new HashMap<String, String>();
        Language.loadFromJson(Files.newInputStream(VanillaLanguageDownloader.getPath(code)), map::put);
        DeprecatedTranslationsInfo.loadFromDefaultResource().applyToMap(map);
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            var modId = mod.getMetadata().getId();
            if (modId.equals("minecraft") || modId.equals("java") || modId.equals("fabric-loader")) {
                continue;
            }

            for (var root : mod.getRootPaths()) {
                var assets = root.resolve("assets");
                if (Files.exists(assets)) {
                    try (var namespaced = Files.list(assets)) {
                        for (var x : namespaced.toList()) {
                            var langPath = x.resolve("lang/" + code + ".json");
                            if (Files.exists(langPath)) {
                                Language.loadFromJson(Files.newInputStream(langPath), map::put);
                            }
                        }
                    }
                }
            }
        }

        Files.createDirectories(cached.getParent());
        Files.writeString(cached, new GsonBuilder().create().toJson(map));

        return new LanguageHandler(map, ServerLanguage.getLanguage(code));
    }

    public String get(String key, @Nullable String fallback) {
        var x = this.assetLanguage.get(key);
        if (x != null) {
            return x;
        }

        x = serverLanguage.serverTranslations().getOrNull(key);

        if (x != null) {
            return x;
        }

        return Language.getInstance().getOrDefault(key, fallback != null ? fallback : key);
    }

    static {
        try (var paths = Files.walk(FabricLoader.getInstance().getGameDir().resolve(".polydex/runtime_cache/"))) {
            paths.sorted(Comparator.reverseOrder()).forEach(x -> {
                try {
                    Files.delete(x);
                } catch (IOException e) {}
            });
        } catch (IOException e) {

        }
    }
}
