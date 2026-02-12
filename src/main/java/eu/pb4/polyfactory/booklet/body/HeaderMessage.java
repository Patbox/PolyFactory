package eu.pb4.polyfactory.booklet.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.mapcanvas.api.font.DefaultFonts;
import eu.pb4.polyfactory.util.language.LanguageHandler;
import eu.pb4.polyfactory.util.language.TextTranslationUtils;
import eu.pb4.polyfactory.util.language.TextUncenterer;
import eu.pb4.polymer.core.api.other.PolymerMapCodec;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.util.StringRepresentable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Locale;

public record HeaderMessage(Component contents, int width) implements DialogBody {
    public static final MapCodec<HeaderMessage> MAP_CODEC = PolymerMapCodec.ofDialogBody(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ComponentSerialization.CODEC.fieldOf("contents").forGetter(HeaderMessage::contents),
                    Dialog.WIDTH_CODEC.optionalFieldOf("width", 310).forGetter(HeaderMessage::width)
            ).apply(instance, HeaderMessage::new)), HeaderMessage::asVanillaBody);

    public MapCodec<HeaderMessage> mapCodec() {
        return MAP_CODEC;
    }


    public PlainMessage asVanillaBody(PacketContext context) {
        var language = context.getClientOptions() != null ? context.getClientOptions().language() : "en_us";
        var title = Component.literal(" ")
                .append(TextTranslationUtils.toTranslatedComponent(LanguageHandler.get(language), contents))
                .append(" ");
        var sides = TextUncenterer.filler((this.width - DefaultFonts.REGISTRY.getWidth(title, 8) - 8) / 2);
        sides.setStyle(sides.getStyle().withStrikethrough(true).withShadowColor(0));
        return new PlainMessage(Component.empty().append(sides).append(title).append(sides), this.width);
    }
}
