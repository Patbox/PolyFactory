package eu.pb4.polyfactory.booklet;

import com.mojang.datafixers.util.Either;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.DirectTextNode;
import eu.pb4.placeholders.api.node.LiteralNode;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.node.parent.StyledNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import eu.pb4.placeholders.api.parsers.tag.TextTag;
import eu.pb4.placeholders.impl.StringArgOps;
import eu.pb4.polyfactory.booklet.body.AlignedMessage;
import eu.pb4.polyfactory.booklet.body.HeaderMessage;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.world.item.ItemStack;

import java.util.*;


public class PageParser {
    private final NodeParser parser;
    private final ParserContext ctx;
    private Identifier returnValue = null;

    public PageParser(HolderLookup.Provider lookup) {
        this.ctx = ParserContext.of(ParserContext.Key.WRAPPER_LOOKUP, lookup);
        this.parser = NodeParser.builder()
                .quickText()
                .markdown()
                .customTagRegistry(TagRegistry.builderCopyDefault()
                        .add(TextTag.self("nl", "booklet", args -> new LiteralNode("\n")))
                        .add(TextTag.self("item", "booklet", args -> {
                            ItemStack stack = ItemStack.EMPTY;
                            try {
                                stack = ItemStack.CODEC.decode(lookup.createSerializationContext(StringArgOps.INSTANCE), Either.right(args)).getOrThrow().getFirst();
                            } catch (Throwable e) {
                                stack = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(args.getNext("item", "stone"))).getDefaultInstance();
                            }
                            return new DirectTextNode(Component.empty().append(stack.getHoverName())
                                    .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowItem(stack))));
                        }))
                        .add(TextTag.enclosing("polydex", "booklet", (node, args, parser) -> {
                            if (!PolydexCompat.IS_PRESENT) {
                                return TextNode.empty();
                            }

                            var id = args.getNext("id", "");
                            var type = args.getNext("type", "result");
                            var nbt = new CompoundTag();
                            nbt.putString("id", id);
                            nbt.putString("return", returnValue.toString());

                            return new StyledNode(node, Style.EMPTY.withClickEvent(new ClickEvent.Custom(BookletInit.id("booklet/polydex/" + type), Optional.of(nbt))), (StyledNode.HoverData<?>) null, null, null);
                        }))
                        .build())
                .build();
    }

    public BookletPage readPage(Identifier identifier, String page) {
        returnValue = identifier;
        var ctx = ParserContext.of();
        var infoIcon = ItemStack.EMPTY;
        var title = identifier.toString();
        var externalTitle = Optional.<String>empty();
        var categories = new HashSet<Identifier>();

        page = page.replace("\r", "").replace("\n\n", "\n<nl>\n");

        var body = new ArrayList<DialogBody>();

        var contents = new StringBuilder();

        var sectionType = SectionType.BODY;
        var previousLine = "";
        var alignment = AlignedMessage.Align.LEFT;

        var width = 300;

        for (var line : page.split("\n")) {
            var spaceLess = line.replace(" ", "").toLowerCase(Locale.ROOT);
            if (spaceLess.startsWith("###section:")) {
                var type = spaceLess.substring("###section:".length());
                if (type.equals("pageinfo")) {
                    sectionType = SectionType.PAGE_INFO;
                    continue;
                }
            } else if (spaceLess.startsWith("###endsection")) {
                sectionType = SectionType.BODY;
                continue;
            } else if (spaceLess.startsWith("###align:")) {
                var newAlign = alignment;
                if (spaceLess.endsWith("center")) {
                    newAlign = AlignedMessage.Align.CENTER;
                } else if (spaceLess.endsWith("left")) {
                    newAlign = AlignedMessage.Align.LEFT;
                } else if (spaceLess.endsWith("right")) {
                    newAlign = AlignedMessage.Align.RIGHT;
                }

                if (newAlign != alignment) {
                    addBody(body, contents, width, alignment);
                    contents = new StringBuilder();
                    alignment = newAlign;
                }
                continue;
            } else if (spaceLess.startsWith("###width:")) {
                int newWidth = width;
                try {
                    newWidth = Integer.parseInt(spaceLess.substring("###width:".length()));
                } catch (Throwable e) {
                    //
                }
                if (width != newWidth) {
                    addBody(body, contents, width, alignment);
                    contents = new StringBuilder();
                    width = newWidth;
                }
                continue;
            } else if (spaceLess.startsWith("###body")) {
                addBody(body, contents, width, alignment);
                contents = new StringBuilder();
                continue;
            } else if (spaceLess.startsWith("###reset")) {
                addBody(body, contents, width, alignment);
                contents = new StringBuilder();
                width = 300;
                alignment = AlignedMessage.Align.LEFT;
                previousLine = "";
                continue;
            } else if (spaceLess.startsWith("###header:")) {
                addBody(body, contents, width, alignment);
                contents = new StringBuilder();
                body.add(new HeaderMessage(stripAndParse(line.substring(line.indexOf(':') + 1)), 310));
                continue;
            }

            if (sectionType == SectionType.BODY) {
                if (!contents.isEmpty() || !line.strip().equals("<nl>")) {
                    if (!contents.isEmpty() && !previousLine.endsWith("<nl>")) {
                        contents.append(" ");
                    }
                    if (!spaceLess.isEmpty() && contents.isEmpty() && spaceLess.charAt(0) == '-') {
                        contents.append('\n');
                    }
                    contents.append(line);
                }
                previousLine = line;
            } else if (sectionType == SectionType.PAGE_INFO) {
                var split = line.split("=", 2);
                if (split.length != 2) {
                    continue;
                }
                var key = split[0].strip().toLowerCase(Locale.ROOT);
                var value = split[1].strip();

                switch (key) {
                    case "title" -> title = value;
                    case "external_title" -> externalTitle = Optional.of(value);
                    case "category" -> {
                        for (var val : value.toLowerCase(Locale.ROOT).replace(" ", "").split(",")) {
                            var parsed = Identifier.tryParse(val);
                            if (parsed != null) {
                                categories.add(parsed);
                            }
                        }
                    }
                    case "icon" ->
                            infoIcon = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(value)).getDefaultInstance();
                }
            }
        }

        addBody(body, contents, width, alignment);
        return new BookletPage(new BookletPage.Info(identifier, infoIcon, parser.parseText(title, ctx), externalTitle.map(x -> parser.parseText(x, ctx)), categories), body);
    }


    private void addBody(List<DialogBody> body, StringBuilder contents, int width, AlignedMessage.Align align) {
        if (!contents.isEmpty()) {
            body.add(new AlignedMessage(stripAndParse(contents.toString()), width, align));
        }
    }

    private Component stripAndParse(String string) {
        while (string.startsWith("<nl>")) {
            string = string.substring("<nl>".length());
        }
        while (string.endsWith("<nl>")) {
            string = string.substring(0, string.length() - "<nl>".length());
        }
        return parser.parseText(string, ctx);
    }

    private enum SectionType {
        BODY,
        PAGE_INFO
    }
}
