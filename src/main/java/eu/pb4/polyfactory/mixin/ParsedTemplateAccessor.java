package eu.pb4.polyfactory.mixin;

import com.mojang.serialization.DataResult;
import net.minecraft.server.dialog.action.ParsedTemplate;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.server.dialog.action.ParsedTemplate.class)
public interface ParsedTemplateAccessor {
    @Invoker
    static DataResult<ParsedTemplate> callParse(String input) {
        throw new UnsupportedOperationException();
    }
}
