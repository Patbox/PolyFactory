package eu.pb4.polyfactory.booklet;

import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.booklet.body.AlignedItemBody;
import eu.pb4.polyfactory.booklet.body.AlignedMessage;
import eu.pb4.polyfactory.booklet.body.HeaderMessage;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.language.TextUncenterer;
import eu.pb4.sgui.api.GuiHelpers;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BookletInit {
    public static final HashMap<Identifier, Map<String, BookletPage>> PAGES = new HashMap<>();
    public static final Map<Identifier, List<Identifier>> CATEGORIES = new HashMap<>();
    public static final Booklet POLYFACTORY = new Booklet(Component.translatable("item.polyfactory.guidebook"),
            List.of(
                    id("basics"),
                    id("rotation"),
                    id("item_transport"),
                    id("fluids"),
                    id("cable")
            )
    );

    public static void init() {
        Registry.register(BuiltInRegistries.DIALOG_BODY_TYPE, id("aligned_message"), AlignedMessage.MAP_CODEC);
        Registry.register(BuiltInRegistries.DIALOG_BODY_TYPE, id("header_message"), HeaderMessage.MAP_CODEC);
        Registry.register(BuiltInRegistries.DIALOG_BODY_TYPE, id("aligned_item"), AlignedItemBody.MAP_CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(BookletInit::loadPages);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, s, g) -> loadPages(server));
    }

    public static boolean openMainPage(ServerPlayer player, Booklet booklet) {
        var body = new ArrayList<DialogBody>();
        var lang = player.clientInformation().language();

        var sideFiller = Component.literal(("" + GuiTextures.INVIS_LINE_RAW).repeat(6)).setStyle(UiResourceCreator.STYLE);

        for (var car : booklet.categories()) {
            body.add(new HeaderMessage(Component.translatable(car.toLanguageKey("booklet_category")), 310));

            for (var id : CATEGORIES.getOrDefault(car, List.of())) {
                var langBox = PAGES.get(id);
                var page = langBox.get(lang);
                if (page == null) {
                    page = langBox.get("en_us");
                    if (page == null) {
                        continue;
                    }
                }

                var plainBody = new PlainMessage(Component.empty()
                        .append(sideFiller)
                        .append(TextUncenterer.getLeftAligned(page.info().getExternalTitle(), 290 - 8 - 2 * 6 * 2, lang).getFirst())
                        .append(sideFiller)
                        .setStyle(Style.EMPTY.withClickEvent(new ClickEvent.Custom(id("booklet/open_page"),
                                Optional.of(StringTag.valueOf(page.info().identifier().toString()))))),
                        290);

                body.add(page.info().icon().isEmpty() ? plainBody : new ItemBody(
                        page.info().icon(),
                        Optional.of(plainBody),
                        false, false,
                        1, 17
                ));
            }
        }

        player.openDialog(Holder.direct(new NoticeDialog(new CommonDialogData(booklet.title(), Optional.empty(), true, true,
                DialogAction.CLOSE, body, List.of()),
                new ActionButton(new CommonButtonData(CommonComponents.GUI_DONE, 200), Optional.empty()))));

        return true;
    }

    public static boolean openPage(ServerPlayer player, Identifier id) {
        if (id.getPath().equals("main_page")) {
            return openMainPage(player, POLYFACTORY);
        }

        var langBox = PAGES.get(id);

        var page = langBox.get(player.clientInformation().language());
        if (page == null) {
            page = langBox.get("en_us");
            if (page == null) {
                return false;
            }
            return false;
        }

        player.openDialog(Holder.direct(page.toDialog(DialogAction.WAIT_FOR_RESPONSE, new ActionButton(
                new CommonButtonData(CommonComponents.GUI_BACK, 200), Optional.of(new StaticAction(new ClickEvent.Custom(id("booklet/open_page"),
                Optional.of(StringTag.valueOf("polyfactory:main_page")))))))));
        return true;
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
                            entry.getKey().getPath().substring(
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

    public static Identifier id(String path) {
        return ModInit.id(path);
    }

    public static void openPolydexUsagePage(ServerPlayer player, CompoundTag compoundTag) {
        var entry = Identifier.tryParse(compoundTag.getStringOr("id", ""));
        var returnPage = Identifier.tryParse(compoundTag.getStringOr("return", ""));

        PolydexCompat.openUsagePage(player, entry, () -> {
            if (GuiHelpers.getCurrentGui(player) != null) {
                GuiHelpers.getCurrentGui(player).close();
            }
            openPage(player, returnPage);
        });
    }

    public static void openPolydexResultPage(ServerPlayer player, CompoundTag compoundTag) {
        var entry = Identifier.tryParse(compoundTag.getStringOr("id", ""));
        var returnPage = Identifier.tryParse(compoundTag.getStringOr("return", "main_page"));

        PolydexCompat.openResultPage(player, entry, () -> {
            if (GuiHelpers.getCurrentGui(player) != null) {
                GuiHelpers.getCurrentGui(player).close();
            }
            openPage(player, returnPage);
        });
    }
}
