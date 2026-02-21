package eu.pb4.polyfactory.booklet.textnode;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.node.parent.ParentTextNode;
import net.minecraft.network.chat.Component;

public record LangCheckNode(String language, boolean equals, TextNode... children) implements ParentTextNode {
    @Override
    public boolean isDynamicNoChildren() {
        return true;
    }

    @Override
    public TextNode[] getChildren() {
        return this.children;
    }

    @Override
    public ParentTextNode copyWith(TextNode[] children) {
        return new LangCheckNode(this.language, this.equals, children);
    }

    @Override
    public Component toText(ParserContext context, boolean removeBackslashes) {
        var player = context.contains(PlaceholderContext.KEY) ? context.get(PlaceholderContext.KEY).player() : null;
        var lang = player == null ? "en_us" : player.clientInformation().language();


        if (lang.equals(this.language) == this.equals) {
            return  TextNode.asSingle(this.children).toText(context, removeBackslashes);
        }

        return Component.empty();
    }
}
