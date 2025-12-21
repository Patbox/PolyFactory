package eu.pb4.polyfactory.mixin.datafixer;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import eu.pb4.polyfactory.util.FactoryTypeReferences;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.SequencedMap;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.V3818_3;

@Mixin(V3818_3.class)
public class V3818_3Mixin {
    @Inject(method = "components", at = @At("TAIL"))
    private static void addCustomComponents(Schema schema, CallbackInfoReturnable<SequencedMap<String, Supplier<TypeTemplate>>> cir) {
        var map = cir.getReturnValue();

        map.put("polyfactory:item_filter", () -> DSL.or(References.ITEM_STACK.in(schema), DSL.list(References.ITEM_STACK.in(schema))));
        map.put("polyfactory:stored_data", () -> FactoryTypeReferences.DATA_CONTAINER.in(schema));
        map.put("polyfactory:remote_keys", () -> DSL.compoundList(References.ITEM_STACK.in(schema)));
    }
}
