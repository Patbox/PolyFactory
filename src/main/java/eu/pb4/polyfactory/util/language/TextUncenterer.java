package eu.pb4.polyfactory.util.language;

import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;
import org.apache.commons.lang3.function.TriConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class TextUncenterer {
    public static List<Component> getLeftAligned(Component component, int width, String language) {
        return getAligned(component, width, language, MutableComponent::append);
    }

    public static List<Component> getRightAligned(Component component, int width, String language) {
        return getAligned(component, width, language, (val, filler) -> Component.empty().append(filler).append(val));
    }

    public static List<Component> splitLines(Component component, int width, String language) {
        return getAligned(component, width, language, (val, filler) -> val);
    }

    private static List<Component> getAligned(Component component, int width, String language, BiFunction<MutableComponent, Component, Component> merger) {
        var registry = DefaultFonts.REGISTRY;

        var list = new ArrayList<Component>();

        var obj = new FormattedText.StyledContentConsumer<>() {
            MutableComponent line = Component.empty();

            @Override
            public Optional<Object> accept(Style style, String string) {
                int newLine = string.indexOf('\n');

                var text = newLine != -1 ? string.substring(0, newLine) : string;

                var comp = TextTranslationUtils.componentify(text, style);

                var w = registry.getWidth(Component.empty().append(line).append(comp), 8);

                if (w > width && comp.getContents() instanceof PlainTextContents) {
                    var parts = text.split(" ");
                    if (parts.length == 1) {
                        if (!line.getSiblings().isEmpty()
                                && line.getSiblings().getLast().getContents() instanceof PlainTextContents contents
                                && contents.text().equals(" ")) {
                            line.getSiblings().removeLast();
                        }

                        list.add(merger.apply(line, filler(width - registry.getWidth(line, 8))));
                        line = Component.empty();
                        line.append(comp);
                    } else {
                        for (var part : parts) {
                            accept(style, part);
                            //noinspection StringEquality
                            if (!line.getSiblings().isEmpty() && parts[parts.length - 1] != part) {
                                accept(style, " ");
                            }
                        }
                    }
                } else if (w > width) {
                    if (!line.getSiblings().isEmpty()
                            && line.getSiblings().getLast().getContents() instanceof PlainTextContents contents
                            && contents.text().equals(" ")) {
                        line.getSiblings().removeLast();
                    }
                    list.add(merger.apply(line, filler(width - registry.getWidth(line, 8))));

                    line = Component.empty();
                    line.append(comp);
                } else {
                    line.append(comp);
                }

                if (newLine != -1) {
                    if (!line.getSiblings().isEmpty()
                            && line.getSiblings().getLast().getContents() instanceof PlainTextContents contents
                            && contents.text().equals(" ")) {
                        line.getSiblings().removeLast();
                    }
                    list.add(merger.apply(line, filler(width - registry.getWidth(line, 8))));
                    line = Component.empty();
                    accept(style, string.substring(newLine + 1));
                }

                return Optional.empty();
            }
        };


        TextTranslationUtils.visitText(LanguageHandler.get(language), component, obj);

        if (!obj.line.getSiblings().isEmpty()) {
            if (obj.line.getSiblings().getLast().getContents() instanceof PlainTextContents contents
                    && contents.text().equals(" ")) {
                obj.line.getSiblings().removeLast();
            }
            list.add(merger.apply(obj.line, filler(width - registry.getWidth(obj.line, 8))));
        }

        return list;
    }

    public static MutableComponent filler(int width) {
        var b = new StringBuilder();
        while (width > 0) {
            if (width >= 100) {
                b.append(GuiTextures.SPACE_100);
                width -= 100;
            } else if (width >= 50) {
                b.append(GuiTextures.SPACE_50);
                width -= 50;
            } else if (width >= 20) {
                b.append(GuiTextures.SPACE_20);
                width -= 20;
            } else if (width >= 10) {
                b.append(GuiTextures.SPACE_10);
                width -= 10;
            } else if (width >= 5) {
                b.append(GuiTextures.SPACE_5);
                width -= 5;
            } else {
                b.append(GuiTextures.SPACE_1);
                width -= 1;
            }
        }
        return Component.literal(b.toString()).setStyle(UiResourceCreator.STYLE);
    }
}
