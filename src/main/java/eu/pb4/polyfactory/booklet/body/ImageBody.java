package eu.pb4.polyfactory.booklet.body;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.booklet.BookletImageHandler;
import eu.pb4.polymer.core.api.other.PolymerMapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Optional;

public record ImageBody(Identifier identifier, Optional<Component> description) implements DialogBody {
    public static final MapCodec<ImageBody> MAP_CODEC = PolymerMapCodec.ofDialogBody(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC.fieldOf("image").forGetter(ImageBody::identifier),
                    ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(ImageBody::description)
            ).apply(instance, ImageBody::new)), ImageBody::asVanillaBody);

    public MapCodec<ImageBody> mapCodec() {
        return MAP_CODEC;
    }


    public PlainMessage asVanillaBody(PacketContext context) {
        var image = BookletImageHandler.getImage(this.identifier);
        var comp = image.component();
        if (description.isPresent()) {
            comp = Component.empty().append(comp).append("\n").append(description.get());
        }

        return new PlainMessage(comp, Math.min(image.width(), 1024));
    }
}
