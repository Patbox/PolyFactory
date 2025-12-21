package eu.pb4.polyfactory.mixin.datafixer;

import com.mojang.serialization.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;

@SuppressWarnings({"rawtypes", "unchecked"})
@Mixin(ItemStackComponentizationFix.class)
public class ItemStackComponentizationFixMixin {
    @Unique
    private static final Set<String> COLORED = Set.of(
            "polyfactory:artificial_dye",
            "polyfactory:spray_can",
            "polyfactory:cable",
            "polyfactory:colored_lamp",
            "polyfactory:inverted_colored_lamp",
            "polyfactory:caged_lamp",
            "polyfactory:inverted_caged_lamp"
    );
    @Inject(method = "fixItemStack", at = @At("TAIL"))
    private static void fixCustomStacks(ItemStackComponentizationFix.ItemStackData data, Dynamic dynamic, CallbackInfo ci) {
        if (data.is(COLORED)) {
            data.moveTagToComponent("color", "polyfactory:color");
        }

        if (data.is("polyfactory:spray_can")) {
            data.moveTagToComponent("uses", "polyfactory:uses_left");
        }

        if (data.is("polyfactory:item_filter")) {
            data.moveTagToComponent("item", "polyfactory:item_filter");
        }

        if (data.is("polyfactory:data_memory")) {
            data.moveTagToComponent("cached_data", "polyfactory:stored_data");
            data.moveTagToComponent("read_only", "polyfactory:read_only");
        }

        if (data.is("polyfactory:portable_redstone_transmitter")) {
            data.setComponent("polyfactory:remote_keys", dynamic.emptyMap()
                    .setFieldIfPresent("1", data.removeTag("key1").result())
                    .setFieldIfPresent("2", data.removeTag("key2").result())
            );
        }
    }

}
