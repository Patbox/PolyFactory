package eu.pb4.polyfactory.mixin.datafixer;

import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.fix.ItemStackComponentizationFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

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
    @Inject(method = "fixStack", at = @At("TAIL"))
    private static void fixCustomStacks(ItemStackComponentizationFix.StackData data, Dynamic dynamic, CallbackInfo ci) {
        if (data.itemMatches(COLORED)) {
            data.moveToComponent("color", "polyfactory:color");
        }

        if (data.itemEquals("polyfactory:spray_can")) {
            data.moveToComponent("uses", "polyfactory:uses_left");
        }

        if (data.itemEquals("polyfactory:item_filter")) {
            data.moveToComponent("item", "polyfactory:item_filter");
        }

        if (data.itemEquals("polyfactory:data_memory")) {
            data.moveToComponent("cached_data", "polyfactory:stored_data");
            data.moveToComponent("read_only", "polyfactory:read_only");
        }

        if (data.itemEquals("polyfactory:portable_redstone_transmitter")) {
            data.setComponent("polyfactory:remote_keys", dynamic.emptyMap()
                    .setFieldIfPresent("1", data.getAndRemove("key1").result())
                    .setFieldIfPresent("2", data.getAndRemove("key2").result())
            );
        }
    }

}
