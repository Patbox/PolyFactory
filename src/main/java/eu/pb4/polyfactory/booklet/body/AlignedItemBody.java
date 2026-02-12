package eu.pb4.polyfactory.booklet.body;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polymer.core.api.other.PolymerMapCodec;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.ItemBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Optional;

public record AlignedItemBody(ItemStack item, AlignedMessage description, boolean showDecorations, boolean showTooltip, int width, int height) implements DialogBody {
    public static final MapCodec<AlignedItemBody> MAP_CODEC = PolymerMapCodec.ofDialogBody(RecordCodecBuilder.mapCodec((instance) -> instance.group(
            ItemStack.STRICT_CODEC.fieldOf("item").forGetter(AlignedItemBody::item),
            AlignedMessage.CODEC.fieldOf("description").forGetter(AlignedItemBody::description),
            Codec.BOOL.optionalFieldOf("show_decorations", true).forGetter(AlignedItemBody::showDecorations),
            Codec.BOOL.optionalFieldOf("show_tooltip", true).forGetter(AlignedItemBody::showTooltip),
            ExtraCodecs.intRange(1, 256).optionalFieldOf("width", 16).forGetter(AlignedItemBody::width),
            ExtraCodecs.intRange(1, 256).optionalFieldOf("height", 16).forGetter(AlignedItemBody::height)
    ).apply(instance, AlignedItemBody::new)), AlignedItemBody::asVanillaBody);

    public ItemBody asVanillaBody(PacketContext context) {
        return new ItemBody(item, Optional.of(description.asVanillaBody(context)), showDecorations, showTooltip, width, height);
    }

    public MapCodec<AlignedItemBody> mapCodec() {
        return MAP_CODEC;
    }
}