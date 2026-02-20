package eu.pb4.polyfactory.booklet;

import eu.pb4.polyfactory.booklet.body.ImageBody;
import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.extras.api.format.font.BitmapProvider;
import eu.pb4.polymer.resourcepack.extras.api.format.font.FontAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.font.SpaceProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Function;

public class BookletImageHandler {
    private static final Map<Identifier, ProcessedImage> IMAGES = new HashMap<>();
    private static final ProcessedImage MISSING = new ProcessedImage(Component.literal("<NO IMAGE>").withStyle(ChatFormatting.DARK_RED), 300, ' ', ' ');

    public static ProcessedImage getImage(Identifier identifier) {
        return IMAGES.getOrDefault(identifier, MISSING);
    }

    public static void init() {
        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(BookletImageHandler::createImages);
    }

    private static void createImages(ResourcePackBuilder builder) {
        var fontId = BookletUtil.id("image_hack");
        var style = Style.EMPTY.withColor(0xFFFFFF).withFont(new FontDescription.Resource(fontId)).withShadowColor(0);
        var character = new char[]{'\u0100'};
        var fontBuilder = FontAsset.builder();
        IMAGES.clear();

        var n1 = 'a';
        fontBuilder.add(SpaceProvider.builder().add(n1, -1));
        fontBuilder.add(SpaceProvider.builder().add('b', 1));

        builder.forEachResource((path, resource) -> {
            var ogpath = path;
            if (!path.startsWith("assets/")) {
                return;
            }

            path = path.substring("assets/".length());
            var separator = path.indexOf('/');
            if (separator == -1) {
                return;
            }

            var namespace = path.substring(0, separator);
            path = path.substring(separator + 1);
            if (!path.startsWith("textures/booklet/image/") || !path.endsWith(".png")) {
                return;
            }
            var id = Identifier.fromNamespaceAndPath(namespace, path.substring("textures/booklet/image/".length(), path.length() - ".png".length()));
            var imageString = new StringBuilder();
            var b = BitmapProvider.builder(Identifier.fromNamespaceAndPath(namespace, path.substring("textures/".length())));
            var image = resource.asImage();
            b.height(9);
            b.ascent(7);

            var scale = Mth.ceil(image.getWidth() / 292f);
            var dy = 9;
            var dx = Math.min(128 / scale, 16);

            var width = Mth.ceil((double) image.getWidth() / scale / dx) * dx;
            var height = Mth.ceil((double) image.getHeight() / scale / dy) * dy;

            var from = character[0];

            for (var y = 0; y < height; y += dy) {
                var line = new StringBuilder();
                var ix = 0;
                for (; ix < width / 2; ix += dx) {
                    imageString.append('b');
                }

                for (var x = 0; x < width; x += dx) {
                    imageString.append(character[0]);
                    imageString.append('a');
                    line.append(character[0]++);
                    if (character[0] >= 0x0600 && character[0] < 0x0700) {
                        character[0] = '\u0700';
                    }
                }
                for (; ix < width; ix += dx) {
                    imageString.append('b');
                }
                b.chars(line.toString());

                if (y + dy < height) {
                    imageString.append("\n");
                }
            }
            var to = character[0];

            fontBuilder.add(b);

            IMAGES.put(id, new ProcessedImage(Component.literal(imageString.toString()).setStyle(style), width + width / dx + 8, from, to));

            {
                var newImage = new BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_ARGB);
                var yOffset = (height * scale - image.getHeight()) / 2;
                for (var y = 0; y < image.getHeight(); y++) {
                    for (var x = 0; x < image.getWidth(); x++) {
                        var color = image.getRGB(x, y);
                        if (ARGB.alpha(color) == 0) {
                            color |= 0x01000000;
                        }
                        newImage.setRGB(x, y + yOffset, color);
                    }
                }
                builder.addData(ogpath, PackResource.fromImage(newImage));
            }
        });

        builder.addData("assets/polyfactory/font/image_hack.json", fontBuilder.build());
    }

    public static List<DialogBody> getAllImages() {
        return IMAGES.entrySet().stream().sorted(Comparator.comparing(((Function<Map.Entry<Identifier, ProcessedImage>, ProcessedImage>) Map.Entry::getValue).andThen(ProcessedImage::from)))
                .map(x -> (DialogBody) new ImageBody(x.getKey(),
                Optional.of(Component.literal(x.getKey() + " <0x" + Integer.toString(x.getValue().from, 16) + ", 0x" + Integer.toString(x.getValue().to, 16) + ">" )))).toList();
    }



    public record ProcessedImage(Component component, int width, int from, int to) {
    }
}
