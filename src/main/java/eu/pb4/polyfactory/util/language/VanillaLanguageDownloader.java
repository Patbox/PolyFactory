package eu.pb4.polyfactory.util.language;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class VanillaLanguageDownloader {
    private static final Path BASE_PATH = FabricLoader.getInstance().getGameDir().resolve(".polydex/");
    private static final Path LANG_STORAGE_PATH = BASE_PATH.resolve("vanilla_translations");
    private static final Path LANG_INFO_PATH = BASE_PATH.resolve("language.json");

    public static boolean isReady = false;
    public static boolean downloading = false;

    public static Path getPath(String code) {
        if (code.equals("en_us")) {
            return FabricLoader.getInstance().getModContainer("minecraft").orElseThrow().findPath("assets/minecraft/lang/en_us.json").orElseThrow();
        }

        return LANG_STORAGE_PATH.resolve(code + ".json");
    }

    public static void markReady() {
        isReady = true;
        downloading = false;
    }
    public static void setup() {
        isReady = false;
        downloading = true;
        CompletableFuture.supplyAsync(() -> {
            try {
                return checkAndDownload();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }).handle((val, thr) -> {
            downloading = false;
            if (thr != null) {
                return false;
            }
            return isReady = val;
        });
    }

    private static boolean checkAndDownload() throws Throwable {
        var gson = new GsonBuilder().create();

        var langInfo = new LangInfo();
        if (Files.exists(LANG_STORAGE_PATH) && Files.exists(LANG_INFO_PATH)) {
            try {
                langInfo = gson.fromJson(Files.readString(LANG_INFO_PATH), LangInfo.class);

                if (langInfo.version.equals(SharedConstants.getCurrentVersion().id())) {
                    return true;
                }
                langInfo.version = SharedConstants.getCurrentVersion().id();
            } catch (Throwable ignored) {}
        }
        Files.createDirectories(LANG_STORAGE_PATH);


        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build()) {
            var manifest = gson.fromJson(client.send(
                    HttpRequest.newBuilder().uri(
                            URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
                    ).build(), HttpResponse.BodyHandlers.ofString()).body(), VersionManifest.class);

            var mcVer = SharedConstants.getCurrentVersion().id();

            var version = manifest.versions.stream().filter(x -> x.id.equals(mcVer)).findFirst();
            if (version.isEmpty()) {
                return false;
            }

            if (langInfo.versionHash.equals(version.get().sha1)) {
                Files.writeString(LANG_INFO_PATH, gson.toJson(langInfo));
                return true;
            }

            langInfo.versionHash = version.get().sha1;

            var versionData = gson.fromJson(client.send(
                    HttpRequest.newBuilder().uri(
                            URI.create(version.get().url)
                    ).build(), HttpResponse.BodyHandlers.ofString()).body(), VersionData.class);

            if (langInfo.assetIndexHash.equals(versionData.assetIndex.sha1)) {
                Files.writeString(LANG_INFO_PATH, gson.toJson(langInfo));
                return true;
            }
            langInfo.assetIndexHash = versionData.assetIndex.sha1;
            var assetIndex = gson.fromJson(client.send(
                    HttpRequest.newBuilder().uri(
                            URI.create(versionData.assetIndex.url)
                    ).build(), HttpResponse.BodyHandlers.ofString()).body(), AssetIndex.class);


            var packMcMetaHash = assetIndex.objects.get("pack.mcmeta").hash;
            var file = gson.fromJson(client.send(HttpRequest.newBuilder().uri(
                    URI.create("https://resources.download.minecraft.net/" +  packMcMetaHash.substring(0, 2) + "/" + packMcMetaHash)
            ).build(), HttpResponse.BodyHandlers.ofString()).body(), VanillaLanguageDownloader.PackMcMeta.class);

            if (file.language.isEmpty()) {
                return false;
            }

            for (var x : file.language.keySet()) {
                var val = assetIndex.objects.get("minecraft/lang/" + x + ".json");
                if (val != null && !val.hash.equals(langInfo.langHash.get(x))) {
                    var url = "https://resources.download.minecraft.net/" + val.hash.substring(0, 2) + "/" + val.hash;

                    client.send(HttpRequest.newBuilder().uri(
                            URI.create(url)
                    ).build(), HttpResponse.BodyHandlers.ofFile(LANG_STORAGE_PATH.resolve(x + ".json")));
                    langInfo.langHash.put(x, val.hash);
                }
            }
            Files.writeString(LANG_INFO_PATH, gson.toJson(langInfo));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return true;
    }

    private static class LangInfo {
        public String version = SharedConstants.getCurrentVersion().id();
        public String assetIndexHash = "";
        public String versionHash = "";
        public Map<String, String> langHash = new HashMap<>();
    }

    private static class Version {
        public String id = "";
        public String url = "";
        public String sha1 = "";
    }

    private static class VersionManifest {
        public List<Version> versions = List.of();
    }

    private static class AssetIndexPointer {
        public String url= "";
        public String sha1 = "";
    }

    private static class VersionData {
        public AssetIndexPointer assetIndex;
    }

    private static class AssetIndexEntry {
        public String hash = "";
    }

    private static class AssetIndex {
        public Map<String, AssetIndexEntry> objects = Map.of();
    }

    private static class PackMcMeta {
        public Map<String, JsonObject> language = Map.of();
    }
}
