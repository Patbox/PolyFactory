package eu.pb4.polyfactory.util.language;

import eu.pb4.polyfactory.mixin.TranslatableContentsAccessor;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ResolvableProfile;
import xyz.nucleoid.packettweaker.PacketContext;
import xyz.nucleoid.server.translations.api.LocalizationTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TextTranslationUtils {
    public static List<String> toTranslatedString(List<Component> texts, ServerPlayer player) {
        var arr = new ArrayList<String>(texts.size());
        for (var text : texts) {
            arr.add(stringifyText(text, player).toLowerCase(Locale.ROOT));
        }
        return arr;
    }

    public static String stringifyText(Component input, ServerPlayer player) {
        var code = LocalizationTarget.of(player).getLanguageCode();
        if (code == null || code.equals(LocalizationTarget.ofSystem().getLanguageCode())) {
            return input.getString();
        }

        var b = new StringBuilder();
        var lang = LanguageHandler.get(code);
        visitText(lang, input, (s) -> {
            b.append(s);
            return Optional.empty();
        });
        return b.toString();
    }

    public static void visitText(PacketContext context, Component text, FormattedText.ContentConsumer<?> visitor) {
        visitText(LanguageHandler.get(context.getClientOptions() != null ? context.getClientOptions().language() : "en_us"), text, visitor);
    }
    public static void visitText(LanguageHandler lang, Component text, FormattedText.ContentConsumer<?> visitor) {
        if (text.getContents() instanceof TranslatableContents content) {
            var translation = lang.get(content.getKey(), content.getFallback());
            if (content.getArgs().length == 0) {
                visitor.accept(translation);
            } else {
                ((TranslatableContentsAccessor) content).callDecomposeTemplate(translation, stringVisitable -> {
                    if (stringVisitable instanceof Component text1) {
                        visitText(lang, text1, visitor);
                    } else {
                        stringVisitable.visit(visitor);
                    }
                });
            }
        } else {
            text.getContents().visit(visitor);
        }
        for (var s : text.getSiblings()) {
            visitText(lang, s, visitor);
        }
    }

    public static void visitText(PacketContext context, Component text, FormattedText.StyledContentConsumer<?> visitor) {
        visitText(LanguageHandler.get(context.getClientOptions() != null ? context.getClientOptions().language() : "en_us"), text, visitor);
    }
    public static void visitText(LanguageHandler lang, Component text, FormattedText.StyledContentConsumer<?> visitor) {
        visitText(lang, text, visitor, Style.EMPTY);
    }

    public static void visitText(LanguageHandler lang, Component text, FormattedText.StyledContentConsumer<?> visitor, Style higherStyle) {
        Style style2 = text.getStyle().applyTo(higherStyle);
        if (text.getContents() instanceof TranslatableContents content) {
            var translation = lang.get(content.getKey(), content.getFallback());
            if (content.getArgs().length == 0) {
                visitor.accept(style2, translation);
            } else {
                ((TranslatableContentsAccessor) content).callDecomposeTemplate(translation, stringVisitable -> {
                    if (stringVisitable instanceof Component text1) {
                        visitText(lang, text1, visitor, style2);
                    } else {
                        stringVisitable.visit(visitor, style2);
                    }
                });
            }
        } else {
            text.getContents().visit(visitor, style2);
        }
        for (var s : text.getSiblings()) {
            visitText(lang, s, visitor, style2);
        }
    }

    public static Component toTranslatedComponent(LanguageHandler language, Component contents) {
        var out = Component.empty();
        visitText(language, contents, (style, string) -> {
            out.append(componentify(string, style));
            return Optional.empty();
        });
        return out;
    }

    public static MutableComponent componentify(String string, Style style) {
        if (style.getFont() instanceof FontDescription.AtlasSprite(Identifier atlasId, Identifier spriteId)) {
            return Component.object(new AtlasSprite(atlasId, spriteId)).setStyle(style);
        } else if (style.getFont() instanceof FontDescription.PlayerSprite(ResolvableProfile profile, boolean hat)) {
            return Component.object(new PlayerSprite(profile, hat)).setStyle(style);
        } else {
            return Component.literal(string).setStyle(style);
        }
    }
}
