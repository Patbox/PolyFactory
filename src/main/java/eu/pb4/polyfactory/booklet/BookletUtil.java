package eu.pb4.polyfactory.booklet;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.booklet.body.HeaderMessage;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.language.TextUncenterer;
import eu.pb4.sgui.api.GuiHelpers;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookletUtil {
    public static Identifier id(String path) {
        return ModInit.id(path);
    }

    public static Identifier action(String path) {
        return ModInit.id("booklet/" + path);
    }

    public static ClickEvent.Custom encodeClickEvent(String type) {
        return new ClickEvent.Custom(action(type), Optional.empty());
    }

    public static ClickEvent.Custom encodeClickEvent(String type, Identifier entry, BookletOpenState state) {
        var nbt = state.encode(new CompoundTag());
        if (entry != null) {
            nbt.putString("entry", entry.toString());
        }
        return new ClickEvent.Custom(action(type), Optional.of(nbt));
    }

    public static boolean openMainPage(ServerPlayer player, Booklet booklet, BookletOpenState state) {
        var body = new ArrayList<DialogBody>();
        var lang = player.clientInformation().language();

        var sideFiller = Component.literal(("" + GuiTextures.INVIS_LINE_RAW).repeat(6)).setStyle(UiResourceCreator.STYLE);

        var newState = state.pushPage(id("main_page"));

        for (var car : booklet.categories()) {
            var entries = BookletInit.CATEGORIES.getOrDefault(car, List.of());
            if (entries.isEmpty()) {
                continue;
            }
            body.add(new HeaderMessage(Component.translatable(car.toLanguageKey("booklet_category")), 310));

            for (var id : entries) {
                var langBox = BookletInit.PAGES.get(id);
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
                        .setStyle(Style.EMPTY.withClickEvent(encodeClickEvent("open_page", page.info().identifier(), newState))),
                        290);

                body.add(page.info().icon().isEmpty() ? plainBody : new ItemBody(
                        page.info().icon(),
                        Optional.of(plainBody),
                        false, false,
                        1, 17
                ));
            }
        }

        player.openDialog(Holder.direct(state.getDialog(booklet.title(), body)));

        return true;
    }

    public static boolean openPage(ServerPlayer player, Identifier id, BookletOpenState state) {
        if (id.getPath().equals("main_page")) {
            return openMainPage(player, BookletInit.POLYFACTORY, state);
        } else if (id.getPath().equals("image_test")) {
            player.openDialog(Holder.direct(state.getDialog(Component.literal("Image Test"), BookletImageHandler.getAllImages())));
            return true;
        }

        var langBox = BookletInit.PAGES.get(id);

        if (langBox == null) {
            return false;
        }

        var page = langBox.get(player.clientInformation().language());
        if (page == null) {
            page = langBox.get("en_us");
            if (page == null) {
                return false;
            }
            return false;
        }

        player.openDialog(Holder.direct(page.toDialog(ParserContext.of(BookletOpenState.KEY, state.pushPage(id)), state)));
        return true;
    }

    public static void openPolydexUsagePage(ServerPlayer player, Identifier identifier, BookletOpenState state) {
        var returnState = state.popPage();

        PolydexCompat.openUsagePage(player, identifier, () -> {
            if (GuiHelpers.getCurrentGui(player) != null) {
                GuiHelpers.getCurrentGui(player).close();
            }
            openPage(player, returnState.page(), returnState.state());
        });
    }

    public static void openPolydexResultPage(ServerPlayer player, Identifier identifier, BookletOpenState state) {
        var returnState = state.popPage();

        PolydexCompat.openResultPage(player, identifier, () -> {
            if (GuiHelpers.getCurrentGui(player) != null) {
                GuiHelpers.getCurrentGui(player).close();
            }
            openPage(player, returnState.page(), returnState.state());
        });
    }

    public static void openPolydexCategoryPage(ServerPlayer player, Identifier identifier, BookletOpenState state) {
        var returnState = state.popPage();

        PolydexCompat.openCategoryPage(player, identifier, () -> {
            if (GuiHelpers.getCurrentGui(player) != null) {
                GuiHelpers.getCurrentGui(player).close();
            }
            openPage(player, returnState.page(), returnState.state());
        });
    }
}
