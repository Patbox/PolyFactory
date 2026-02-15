package eu.pb4.polyfactory.booklet;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.placeholders.api.ParserContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.action.StaticAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record BookletOpenState(List<Identifier> previousPages) {
    public static final ParserContext.Key<BookletOpenState> KEY = ParserContext.Key.of("booklet_open_state");
    public static final BookletOpenState DEFAULT = new BookletOpenState(List.of());
    public static MapCodec<BookletOpenState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.listOf().fieldOf("previous_pages").forGetter(BookletOpenState::previousPages)
    ).apply(instance, BookletOpenState::new));

    public static BookletOpenState decode(Optional<Tag> tag) {
        if (tag.isEmpty()) {
            return DEFAULT;
        }

        return MAP_CODEC.decode(NbtOps.INSTANCE, NbtOps.INSTANCE.getMap(tag.get()).getOrThrow()).result().orElse(DEFAULT);
    }

    public BookletOpenState pushPage(Identifier identifier) {
        var list = new ArrayList<>(this.previousPages);
        list.add(identifier);
        return new BookletOpenState(list);
    }

    public PopResult popPage() {
        var list = new ArrayList<>(this.previousPages);
        return new PopResult(new BookletOpenState(list), list.removeLast());
    }

    public boolean isEmpty() {
        return this.previousPages.isEmpty();
    }

    public CompoundTag encode(CompoundTag nbt) {
        var rec = NbtOps.INSTANCE.mapBuilder();
        MAP_CODEC.encode(this, NbtOps.INSTANCE, rec);
        return (CompoundTag) rec.build(nbt).getOrThrow();
    }

    public ActionButton getCloseButton() {
        if (this.previousPages.isEmpty()) {
            return new ActionButton(new CommonButtonData(CommonComponents.GUI_DONE, 200), Optional.empty());
        } else {
            var pop = this.popPage();
            return new ActionButton(new CommonButtonData(CommonComponents.GUI_BACK, 200), Optional.of(new StaticAction(BookletUtil.encodeClickEvent("open_page", pop.page, pop.state))));
        }
    }

    public DialogAction getCloseAction() {
        if (this.previousPages.isEmpty()) {
            return DialogAction.CLOSE;
        } else {
            return DialogAction.WAIT_FOR_RESPONSE;
        }
    }

    public record PopResult(BookletOpenState state, Identifier page) {
    }
}
