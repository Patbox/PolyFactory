package eu.pb4.polyfactory.mixin.datafixer;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import eu.pb4.polyfactory.util.FactoryTypeReferences;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.Schema3818_3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;

@Mixin(Schema3818_3.class)
public class Schema3818_3Mixin {
    @ModifyArg(method = "method_57277", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/DSL;optionalFields([Lcom/mojang/datafixers/util/Pair;)Lcom/mojang/datafixers/types/templates/TypeTemplate;"))
    private static Pair<String, TypeTemplate>[] addCustomComponents(Pair<String, TypeTemplate>[] components,
                                                                    @Local(argsOnly = true) Schema schema) {
        var list = new ArrayList<>(List.of(components));

        list.add(Pair.of("polyfactory:item_filter", TypeReferences.ITEM_STACK.in(schema)));
        list.add(Pair.of("polyfactory:stored_data", FactoryTypeReferences.DATA_CONTAINER.in(schema)));
        list.add(Pair.of("polyfactory:remote_keys", DSL.compoundList(TypeReferences.ITEM_STACK.in(schema))));

        return list.toArray(components);
    }
}
