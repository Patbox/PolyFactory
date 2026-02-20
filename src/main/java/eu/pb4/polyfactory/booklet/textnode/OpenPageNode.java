package eu.pb4.polyfactory.booklet.textnode;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.node.parent.ParentNode;
import eu.pb4.placeholders.api.node.parent.ParentTextNode;
import eu.pb4.polyfactory.booklet.BookletOpenState;
import eu.pb4.polyfactory.booklet.BookletUtil;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public class OpenPageNode extends ParentNode {

    private final Identifier entry;

    public OpenPageNode(Identifier page, TextNode... node) {
        super(node);
        this.entry = page;
    }

    @Override
    protected Style applyFormatting(Style style, ParserContext context) {
        return style.withClickEvent(BookletUtil.encodeClickEvent("open_page", entry, context.getOrThrow(BookletOpenState.KEY), true));
    }

    @Override
    public boolean isDynamicNoChildren() {
        return true;
    }

    @Override
    public ParentTextNode copyWith(TextNode[] children) {
        return new OpenPageNode(entry, children);
    }
}
