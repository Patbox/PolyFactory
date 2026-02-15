package eu.pb4.polyfactory.booklet;

import eu.pb4.polyfactory.booklet.body.AlignedItemBody;
import eu.pb4.polyfactory.booklet.body.AlignedMessage;
import eu.pb4.polyfactory.booklet.body.HeaderMessage;
import eu.pb4.polyfactory.booklet.body.ImageBody;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class BookletInit {
    public static final HashMap<Identifier, Map<String, BookletPage>> PAGES = new HashMap<>();
    public static final Map<Identifier, List<Identifier>> CATEGORIES = new HashMap<>();
    public static final Booklet POLYFACTORY = new Booklet(Component.translatable("item.polyfactory.guidebook"),
            List.of(
                    id("basics"),
                    id("rotation"),
                    id("item_transport"),
                    id("fluids"),
                    id("cable"),
                    id("misc")
            )
    );

    public static void init() {
        Registry.register(BuiltInRegistries.DIALOG_BODY_TYPE, id("aligned_message"), AlignedMessage.MAP_CODEC);
        Registry.register(BuiltInRegistries.DIALOG_BODY_TYPE, id("header_message"), HeaderMessage.MAP_CODEC);
        Registry.register(BuiltInRegistries.DIALOG_BODY_TYPE, id("aligned_item"), AlignedItemBody.MAP_CODEC);
        Registry.register(BuiltInRegistries.DIALOG_BODY_TYPE, id("image"), ImageBody.MAP_CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(BookletInit::loadPages);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, s, g) -> loadPages(server));

        BookletImageHandler.init();
    }

    private static void loadPages(MinecraftServer server) {
        try {
            var pages = server.getResourceManager().listResources("booklet/pages", x -> x.getPath().endsWith(".txt") && x.getPath().indexOf('/') != -1);
            PAGES.clear();
            CATEGORIES.clear();
            var parser = new PageParser(server.registryAccess());
            for (var entry : pages.entrySet()) {
                try {
                    var pathWithLang = entry.getKey().getPath().substring("booklet/pages/".length());
                    var langIndex = pathWithLang.indexOf('/');
                    var lang = pathWithLang.substring(0, langIndex);

                    var id = entry.getKey().withPath(
                            pathWithLang.substring(
                                    langIndex + 1,
                                    pathWithLang.length() - ".txt".length()
                            )
                    );
                    var string = new String(entry.getValue().open().readAllBytes(), StandardCharsets.UTF_8);
                    var page = parser.readPage(id, string);
                    PAGES.computeIfAbsent(id, x -> new HashMap<>()).put(lang, page);

                    for (var cat : page.info().categories()) {
                        var list = CATEGORIES.computeIfAbsent(cat, c -> new ArrayList<>());
                        if (!list.contains(id)) {
                            list.add(id);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void handleAction(ServerPlayer player, String path, Optional<Tag> payload) {
        var state = BookletOpenState.decode(payload);
        Identifier entry = Identifier.tryParse("");
        if (payload.isPresent() && payload.get() instanceof CompoundTag tag) {
            entry = Identifier.tryParse(tag.getStringOr("entry", ""));
        }

        switch (path) {
            case "open_page" -> BookletUtil.openPage(player, entry, state);
            case "polydex/usage" -> BookletUtil.openPolydexUsagePage(player, entry, state);
            case "polydex/result" -> BookletUtil.openPolydexResultPage(player, entry, state);
            case "close" -> player.closeContainer();
        }
    }
}
