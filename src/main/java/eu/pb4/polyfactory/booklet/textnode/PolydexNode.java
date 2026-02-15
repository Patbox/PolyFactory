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

public class PolydexNode extends ParentNode {

    private final Identifier entry;
    private final String type;

    public PolydexNode(Identifier entry, String type, TextNode... node) {
        super(node);
        this.entry = entry;
        this.type = type;
    }

    @Override
    protected Style applyFormatting(Style style, ParserContext context) {
        if (!PolydexCompat.IS_PRESENT) {
            return style.withColor(ChatFormatting.DARK_RED)
                    .withStrikethrough(true).withHoverEvent(new HoverEvent.ShowText(Component.translatable("text.polyfactory.polydex_required_to_work")));
        }

        return style.withClickEvent(BookletUtil.encodeClickEvent("polydex/" + type, entry, context.getOrThrow(BookletOpenState.KEY)));
    }

    @Override
    public boolean isDynamicNoChildren() {
        return true;
    }

    @Override
    public ParentTextNode copyWith(TextNode[] children) {
        return new PolydexNode(entry, type, children);
    }
}
