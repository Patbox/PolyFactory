package eu.pb4.polyfactory.booklet;

import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Either;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.arguments.SimpleArguments;
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
import eu.pb4.polyfactory.booklet.textnode.LangCheckNode;
import eu.pb4.polyfactory.booklet.textnode.OpenPageNode;
import eu.pb4.polyfactory.booklet.textnode.PolydexNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
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
                .globalPlaceholders()
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
                        .add(TextTag.enclosing("iflang", "booklet", (node, args, parser) -> new LangCheckNode(
                                args.getNext("language", "en_us"), SimpleArguments.bool(args.getNext("equals", "true"), true),
                                node)))
                        .build())
                .build();
    }

    public BookletPage readPage(Identifier identifier, String page) {
        currentPage = identifier;
        var ctx = ParserContext.of();
        var infoIcon = ItemStack.EMPTY;
        var title = identifier.toString();
        var externalTitle = Optional.<String>empty();
        var description = Optional.<String>empty();
        var categories = new HashSet<Identifier>();

        page = page.replace("\r", "")
                .replace(' ', ' ')
                .replace('\u00A0', ' ')
                .replace("\n\n", "\n<nl>\n");

        var body = new BodyBuilder(this, new ArrayList<>());

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
                    body.add(contents, width, alignment);
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
                    body.add(contents, width, alignment);
                    contents = new StringBuilder();
                    width = newWidth;
                }
                continue;
            } else if (spaceLess.startsWith("###body")) {
                body.add(contents, width, alignment);
                contents = new StringBuilder();
                continue;
            } else if (spaceLess.startsWith("###reset")) {
                body.add(contents, width, alignment);
                contents = new StringBuilder();
                width = 300;
                alignment = AlignedMessage.Align.LEFT;
                previousLine = "";
                continue;
            } else if (spaceLess.startsWith("###header:")) {
                body.add(contents, width, alignment);
                contents = new StringBuilder();
                body.add(stripAndParse(line.substring(line.indexOf(':') + 1)), x -> new HeaderMessage(x, 310));
                continue;
            } else if (spaceLess.startsWith("###image:")) {
                body.add(contents, width, alignment);
                contents = new StringBuilder();
                var args = line.substring(line.indexOf(':') + 1).strip();
                var spaceIndex = args.indexOf(' ');
                TextNode imageDescription = null;
                if (spaceIndex != -1) {
                    imageDescription = stripAndParse(args.substring(spaceIndex + 1));
                    args = args.substring(0, spaceIndex);
                }

                var id = Identifier.tryParse(args);
                if (id != null) {
                    if (imageDescription != null) {
                        body.add(imageDescription, x -> new ImageBody(id, Optional.of(x)));
                    } else {
                        var out = new ImageBody(id, Optional.empty());
                        body.add(x -> out);
                    }
                }
                continue;
            } else if (spaceLess.startsWith("###categoryentries:")) {
                var id = Identifier.tryParse(spaceLess.substring(spaceLess.indexOf(':') + 1));
                body.add(contents, width, alignment);
                contents = new StringBuilder();
                if (id != null) {
                    body.addDynamicMulti(x -> BookletUtil.getCategoryBodyList(id, x));
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
                    case "description" -> description = Optional.of(value);
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

        body.add(contents, width, alignment);
        return new BookletPage(new BookletPage.Info(identifier, infoIcon, parser.parseText(title, ctx), externalTitle.map(x -> parser.parseText(x, ctx)),
                description.map(x -> parser.parseText(x, ctx)), categories), body.list);
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

    public record BodyBuilder(PageParser pageParser, List<Function<ParserContext, List<DialogBody>>> list) {
        public void addDynamicMulti(Function<ParserContext, List<DialogBody>> function) {
            this.list.add(function);
        }

        public void add(Function<ParserContext, DialogBody> function) {
            this.list.add(function.andThen(List::of));
        }

        public void add(TextNode node, Function<Component, DialogBody> function) {
            if (node.isDynamic()) {
                list.add(x -> List.of(function.apply(node.toText(x))));
            } else {
                var out = List.of(function.apply(node.toText(pageParser.ctx)));
                list.add(x -> out);
            }
        }

        public void add(StringBuilder contents, int width, AlignedMessage.Align align) {
            if (!contents.isEmpty()) {
                var node = pageParser.stripAndParse(contents.toString());
                if (node.isDynamic()) {
                    list.add(x -> {
                        var y = node.toText(x);
                        return y.getSiblings().isEmpty() && y.getContents() instanceof PlainTextContents plainTextContents && plainTextContents.text().isEmpty()
                                ? List.of()
                                : List.of(new AlignedMessage(y, width, align));
                    });
                } else {
                    var y = node.toText(this.pageParser.ctx);
                    if (!y.getSiblings().isEmpty() || !(y.getContents() instanceof PlainTextContents plainTextContents && plainTextContents.text().isEmpty())) {
                        List<DialogBody> z = List.of(new AlignedMessage(y, width, align));
                        this.list.add(x -> z);
                    }
                }
            }
        }
    }
}
