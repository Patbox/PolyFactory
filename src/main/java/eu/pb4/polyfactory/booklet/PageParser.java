package eu.pb4.polyfactory.booklet;

import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Either;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.DirectTextNode;
import eu.pb4.placeholders.api.node.LiteralNode;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import eu.pb4.placeholders.api.parsers.tag.TextTag;
import eu.pb4.placeholders.impl.StringArgOps;
import eu.pb4.polyfactory.booklet.body.AlignedMessage;
import eu.pb4.polyfactory.booklet.body.HeaderMessage;
import eu.pb4.polyfactory.booklet.body.ImageBody;
import eu.pb4.polyfactory.booklet.textnode.OpenPageNode;
import eu.pb4.polyfactory.booklet.textnode.PolydexNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Function;


public class PageParser {
    private final NodeParser parser;
    private final ParserContext ctx;
    private final ItemParser itemParser;
    private Identifier currentPage = null;

    public PageParser(HolderLookup.Provider lookup) {
        this.ctx = ParserContext.of(ParserContext.Key.WRAPPER_LOOKUP, lookup);
        this.itemParser = new ItemParser(lookup);
        this.parser = NodeParser.builder()
                .quickText()
                .markdown()
                .customTagRegistry(TagRegistry.builderCopyDefault()
                        .add(TextTag.self("nl", "booklet", args -> new LiteralNode("\n")))
                        .add(TextTag.self("nl2", "booklet", args -> new LiteralNode("\n\n")))
                        .add(TextTag.self("item", "booklet", args -> {
                            ItemStack stack = ItemStack.EMPTY;
                            try {
                                stack = ItemStack.CODEC.decode(lookup.createSerializationContext(StringArgOps.INSTANCE), Either.right(args)).getOrThrow().getFirst();
                            } catch (Throwable e) {
                                stack = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(args.getNext("item", "stone"))).getDefaultInstance();
                            }
                            return new DirectTextNode(stack.getHoverName());
                        }))
                        .add(TextTag.self("citem", "booklet", args -> {
                            ItemStack stack = ItemStack.EMPTY;
                            try {
                                stack = ItemStack.CODEC.decode(lookup.createSerializationContext(StringArgOps.INSTANCE), Either.right(args)).getOrThrow().getFirst();
                            } catch (Throwable e) {
                                stack = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(args.getNext("item", "stone"))).getDefaultInstance();
                            }
                            return new DirectTextNode(Component.empty().append(stack.getHoverName()).setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
                        }))
                        .add(TextTag.enclosing("polydex", "booklet", (node, args, parser) -> {
                            var id = args.getNext("id", "");
                            var type = args.getNext("type", "result");

                            return new PolydexNode(Identifier.tryParse(id), type, node);
                        }))
                        .add(TextTag.enclosing("pagelink", "booklet", (node, args, parser) -> {
                            var page = args.getNext("id", "");
                            Identifier id;
                            if (page.startsWith("/")) {
                                id = currentPage.withPath(page.substring(1));
                            } else if (page.startsWith("./")) {
                                var spit = currentPage.getPath().split("/");
                                spit[spit.length - 1] = page.substring(2);
                                id = currentPage.withPath(String.join("/", spit));
                            } else {
                                id = Identifier.tryParse(page);
                            }

                            return new OpenPageNode(id, node);
                        }))
                        .build())
                .build();
    }

    public BookletPage readPage(Identifier identifier, String page) {
        currentPage = identifier;
        var ctx = ParserContext.of();
        var infoIcon = ItemStack.EMPTY;
        var title = identifier.toString();
        var externalTitle = Optional.<String>empty();
        var categories = new HashSet<Identifier>();

        page = page.replace("\r", "")
                .replace(' ', ' ')
                .replace('\u00A0', ' ')
                .replace("\n\n", "\n<nl>\n");

        var body = new ArrayList<Function<ParserContext, DialogBody>>();

        var contents = new StringBuilder();

        var sectionType = SectionType.BODY;
        var previousLine = "";
        var alignment = AlignedMessage.Align.LEFT;

        var width = 300;

        for (var line : page.lines().toList()) {
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
                addBody(body, stripAndParse(line.substring(line.indexOf(':') + 1)), x -> new HeaderMessage(x, 310));
                continue;
            } else if (spaceLess.startsWith("###image:")) {
                addBody(body, contents, width, alignment);
                contents = new StringBuilder();
                var args = line.substring(line.indexOf(':') + 1).strip();
                var spaceIndex = args.indexOf(' ');
                TextNode description = null;
                if (spaceIndex != -1) {
                    description = stripAndParse(args.substring(spaceIndex + 1));
                    args = args.substring(0, spaceIndex);
                }

                var id = Identifier.tryParse(args);
                if (id != null) {
                    if (description != null) {
                        addBody(body, description, x -> new ImageBody(id, Optional.of(x)));
                    } else {
                        var out = new ImageBody(id, Optional.empty());
                        body.add(x -> out);
                    }
                }
                continue;
            }

            if (sectionType == SectionType.BODY) {
                if (!contents.isEmpty() || (!line.strip().equals("<nl>") && !line.strip().equals("<nl2>"))) {
                    if (!spaceLess.isEmpty() && !contents.isEmpty() && spaceLess.charAt(0) == '-') {
                        contents.append('\n');
                    } else if (!contents.isEmpty() && (!previousLine.endsWith("<nl>") && !previousLine.endsWith("<nl2>"))) {
                        contents.append(" ");
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
                    case "icon" -> {
                        try {
                            var res = this.itemParser.parse(new StringReader(value));
                            infoIcon = new ItemStack(res.item(), 1, res.components());
                        } catch (Throwable e) {
                            // Ignore
                        }
                    }
                }
            }
        }

        addBody(body, contents, width, alignment);
        return new BookletPage(new BookletPage.Info(identifier, infoIcon, parser.parseText(title, ctx), externalTitle.map(x -> parser.parseText(x, ctx)), categories), body);
    }


    private void addBody(List<Function<ParserContext, DialogBody>> body, TextNode node, Function<Component, DialogBody> function) {
        if (node.isDynamic()) {
            body.add(x -> function.apply(node.toText(x)));
        } else {
            var out = function.apply(node.toText(ctx));
            body.add(x -> out);
        }
    }

    private void addBody(List<Function<ParserContext, DialogBody>> body, StringBuilder contents, int width, AlignedMessage.Align align) {
        if (!contents.isEmpty()) {
            var node = stripAndParse(contents.toString());

            addBody(body, node, x -> new AlignedMessage(x, width, align));
        }
    }

    private TextNode stripAndParse(String string) {
        while (string.startsWith("<nl>")) {
            string = string.substring("<nl>".length());
        }
        while (string.endsWith("<nl>")) {
            string = string.substring(0, string.length() - "<nl>".length());
        }
        var out = parser.parseNode(string);

        return out;
    }

    private enum SectionType {
        BODY,
        PAGE_INFO
    }
}
