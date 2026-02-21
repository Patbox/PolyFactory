package eu.pb4.polyfactory.booklet;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.*;
import net.minecraft.server.dialog.action.StaticAction;
import net.minecraft.server.dialog.body.DialogBody;

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

    public Dialog getDialog(Component title, List<DialogBody> body) {
        var buttonWidth = this.previousPages.isEmpty() ? 200 : 96;
        var closeButton = new ActionButton(new CommonButtonData(CommonComponents.GUI_DONE, buttonWidth),
                Optional.of(new StaticAction(BookletUtil.encodeClickEvent("close", null, this))));

        title = Component.empty().append(Component.literal("" + GuiTextures.NEGATIVE_SPACE_2500
                + GuiTextures.DARKENER + GuiTextures.NEGATIVE_SPACE_2500
        ).setStyle(UiResourceCreator.STYLE.withShadowColor(0))).append(title);

        if (this.previousPages.isEmpty()) {
            return new NoticeDialog(new CommonDialogData(title, Optional.empty(), true, false, DialogAction.NONE, body, List.of()),
                    closeButton);
        } else {
            var pop = this.popPage();
            var backButton = new ActionButton(new CommonButtonData(CommonComponents.GUI_BACK, buttonWidth), Optional.of(new StaticAction(BookletUtil.encodeClickEvent("open_page", pop.page, pop.state))));

            return new ConfirmationDialog(new CommonDialogData(title, Optional.empty(), true, false, DialogAction.NONE, body, List.of()),
                    closeButton,
                    backButton
            );
        }
    }
    public record PopResult(BookletOpenState state, Identifier page) {}
}
