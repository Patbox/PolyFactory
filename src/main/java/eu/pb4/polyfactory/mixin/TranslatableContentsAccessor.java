package eu.pb4.polyfactory.mixin;

import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.network.chat.contents.TranslatableContents.class)
public interface TranslatableContentsAccessor {
    @Invoker
    void callDecomposeTemplate(String formatTemplate, Consumer<FormattedText> consumer);
}
