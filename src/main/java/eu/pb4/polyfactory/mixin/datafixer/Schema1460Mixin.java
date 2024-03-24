package eu.pb4.polyfactory.mixin.datafixer;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.Schema1460;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Supplier;

@Mixin(Schema1460.class)
public abstract class Schema1460Mixin extends Schema {
    public Schema1460Mixin(int versionKey, Schema parent) {
        super(versionKey, parent);
    }

    @Shadow
    protected static void method_5273(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
    }

    @Inject(method = "registerBlockEntities", at = @At("RETURN"))
    private void registerPolyFactoryBlockEntities(Schema schema, CallbackInfoReturnable<Map<String, Supplier<TypeTemplate>>> cir) {
        var map = cir.getReturnValue();

        //schema.register(map, mod("conveyor"), (name) -> {
        //    return DSL.optionalFields("HeldStack", TypeReferences.ITEM_STACK.in(schema));
        //});

        method_5273(schema, map, mod("steam_engine"));
        method_5273(schema, map, mod("grinder"));
        method_5273(schema, map, mod("press"));
        method_5273(schema, map, mod("mixer"));
        method_5273(schema, map, mod("crafter"));
        method_5273(schema, map, mod("workbench"));

        container(schema, map, "container");
        container(schema, map, "creative_container");

        stack(schema, map, "funnel", "FilterStack");
        stack(schema, map, "conveyor", "HeldStack");
        stack(schema, map, "item_reader", "stack");

        stackOwner(schema, map, "miner", "tool");
        stackOwner(schema, map, "placer", "stack");
        stackOwner(schema, map, "planter", "stack");

        dataCache(schema, map, "nixie_tube_controller");
        dataCache(schema, map, "provider_data_cache");
        dataCache(schema, map, "hologram_projector");

        schema.register(map, mod("splitter"), (name) -> {
            return DSL.optionalFields("FilterStackLeft", TypeReferences.ITEM_STACK.in(schema),
                    "FilterStackRight", TypeReferences.ITEM_STACK.in(schema))
                    ;
        });
        schema.register(map, mod("windmill"), (name) -> {
            return DSL.optionalFields("Sails", DSL.list(TypeReferences.ITEM_STACK.in(schema)))
                    ;
        });
        schema.register(map, mod("wireless_redstone"), (name) -> {
            return DSL.optionalFields("key1", TypeReferences.ITEM_STACK.in(schema),
                    "key2", TypeReferences.ITEM_STACK.in(schema))
                    ;
        });

        schema.registerSimple(map, mod("cable"));
        schema.registerSimple(map, mod("wither_skull_generator"));
        schema.registerSimple(map, mod("electric_motor"));
        schema.registerSimple(map, mod("creative_motor"));
        schema.registerSimple(map, mod("fan"));
        schema.registerSimple(map, mod("nixie_tube"));
        schema.registerSimple(map, mod("hand_crank"));
    }

    @Unique
    private static void stack(Schema schema, Map<String, Supplier<TypeTemplate>> map, String path, String nbt) {
        schema.register(map, mod(path), (name) -> {
            return DSL.optionalFields(nbt, TypeReferences.ITEM_STACK.in(schema));
        });
    }

    @Unique
    private static void stackOwner(Schema schema, Map<String, Supplier<TypeTemplate>> map, String path, String nbt) {
        schema.register(map, mod(path), (name) -> {
            return DSL.optionalFields(nbt, TypeReferences.ITEM_STACK.in(schema));
        });
    }

    @Unique
    private static void dataCache(Schema schema, Map<String, Supplier<TypeTemplate>> map, String path) {
        schema.registerSimple(map, mod(path));
    }

    @Unique
    private static void container(Schema schema, Map<String, Supplier<TypeTemplate>> map, String path) {
        schema.registerSimple(map, mod(path));
    }

    @Unique
    private static String mod(String path) {
        return "polyfactory:" + path;
    }
}
