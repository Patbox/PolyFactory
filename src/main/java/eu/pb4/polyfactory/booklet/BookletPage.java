package eu.pb4.polyfactory.booklet;

import eu.pb4.placeholders.api.ParserContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public record BookletPage(Info info, List<Function<ParserContext, List<DialogBody>>> body) {
    public record Info(Identifier identifier, ItemStack icon, Component title, Optional<Component> externalTitle, Optional<Component> description, Set<Identifier> categories) {
        public Component getExternalTitle() {
            return this.externalTitle.orElse(title);
        }
    }

    public Dialog toDialog(ParserContext context, BookletOpenState state) {
        return state.getDialog(this.info.title, this.body.stream().map(x -> x.apply(context)).flatMap(List::stream).toList());
    }
}
