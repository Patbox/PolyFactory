package eu.pb4.polyfactory.mixin.datafixer;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.Hook;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import eu.pb4.polyfactory.util.FactoryTypeReferences;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.Schema1460;
import net.minecraft.datafixer.schema.Schema705;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(Schema1460.class)
public abstract class Schema1460Mixin extends Schema {
    @Shadow
    protected static void method_5273(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
    }

    public Schema1460Mixin(int versionKey, Schema parent) {
        super(versionKey, parent);
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
        method_5273(schema, map, mod("blueprint_workbench"));
        method_5273(schema, map, mod("mechanical_drain"));
        method_5273(schema, map, mod("mechanical_spout"));
        method_5273(schema, map, mod("slot_aware_hopper"));

        container(schema, map, "container");
        container(schema, map, "creative_container");

        stack(schema, map, "funnel", "FilterStack");
        stack(schema, map, "conveyor", "HeldStack");
        stack(schema, map, "item_reader", "stack");
        stack(schema, map, "item_packer", "item");
        stack(schema, map, "record_player", "stack");

        stackOwner(schema, map, "miner", "tool");
        stackOwner(schema, map, "placer", "stack");

        dataCache(schema, map, "nixie_tube_controller");
        dataCache(schema, map, "provider_data_cache");
        dataCache(schema, map, "hologram_projector");
        dataCache(schema, map, "data_memory");

        schema.register(map, mod("double_input_transformer"), (name) -> {
            return DSL.optionalFields("input_data_1", FactoryTypeReferences.DATA_CONTAINER.in(schema),
                    "input_data_2", FactoryTypeReferences.DATA_CONTAINER.in(schema),
                    "output_data", FactoryTypeReferences.DATA_CONTAINER.in(schema))
                    ;
        });

        schema.register(map, mod("input_transformer"), (name) -> {
            return DSL.optionalFields("input_data", FactoryTypeReferences.DATA_CONTAINER.in(schema),
                    "output_data", FactoryTypeReferences.DATA_CONTAINER.in(schema))
                    ;
        });

        schema.register(map, mod("data_extractor"), (name) -> {
            return DSL.optionalFields("input_data", FactoryTypeReferences.DATA_CONTAINER.in(schema),
                    "output_data", FactoryTypeReferences.DATA_CONTAINER.in(schema))
                    ;
        });

        schema.register(map, mod("planter"), (name) -> {
            return DSL.optionalFields("stack", TypeReferences.ITEM_STACK.in(schema), "Items", DSL.list(TypeReferences.ITEM_STACK.in(schema)));
        });

        schema.register(map, mod("splitter"), (name) -> {
            return DSL.optionalFields("FilterStackLeft", TypeReferences.ITEM_STACK.in(schema),
                    "FilterStackRight", TypeReferences.ITEM_STACK.in(schema))
                    ;
        });
        schema.register(map, mod("windmill"), (name) -> {
            return DSL.optionalFields("Sails", DSL.list(TypeReferences.ITEM_STACK.in(schema)));
        });
        schema.register(map, mod("wireless_redstone"), (name) -> {
            return DSL.optionalFields("key1", TypeReferences.ITEM_STACK.in(schema),
                    "key2", TypeReferences.ITEM_STACK.in(schema))
                    ;
        });

        schema.registerSimple(map, mod("cable"));
        schema.registerSimple(map, mod("electric_motor"));
        schema.registerSimple(map, mod("creative_motor"));
        schema.registerSimple(map, mod("fan"));
        schema.registerSimple(map, mod("nixie_tube"));
        schema.registerSimple(map, mod("hand_crank"));
        schema.registerSimple(map, mod("pump"));
        schema.registerSimple(map, mod("pipe"));
        schema.registerSimple(map, mod("nozzle"));
        schema.registerSimple(map, mod("drain"));
        schema.registerSimple(map, mod("filtered_pipe"));
        schema.registerSimple(map, mod("redstone_valve_pipe"));
        schema.registerSimple(map, mod("fluid_tank"));
        schema.registerSimple(map, mod("portable_fluid_tank"));
    }

    @Inject(method = "registerTypes", at = @At("HEAD"))
    private void registerTypeRef(Schema schema, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes, CallbackInfo ci) {
        //schema.registerType(false, TypeReferences.GAME_EVENT_NAME, DSL::remainder);
        schema.registerType(true, FactoryTypeReferences.DATA_CONTAINER, () -> {
            return DSL.allWithRemainder(DSL.optional(DSL.taggedChoiceLazy("type", DSL.string(), Map.of(
                    "block_state", () -> DSL.optionalFields("value", TypeReferences.BLOCK_STATE.in(schema)),
                    "item_stack", () -> DSL.optionalFields("value", TypeReferences.ITEM_STACK.in(schema))//,
                //    "game_event", () -> DSL.optionalFields("event", TypeReferences.GAME_EVENT_NAME.in(schema))
            ))));
        });
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
        schema.register(map, mod(path), (name) -> {
            return DSL.optionalFields("data", FactoryTypeReferences.DATA_CONTAINER.in(schema));
        });
    }

    @Unique
    private static void container(Schema schema, Map<String, Supplier<TypeTemplate>> map, String path) {
        schema.register(map, mod(path), (name) -> {
            return DSL.optionalFields("variant",
                    DSL.optionalFields(
                            "item", TypeReferences.ITEM_NAME.in(schema),
                            "components", TypeReferences.DATA_COMPONENTS.in(schema)
                    ));
        });    }

    @Unique
    private static String mod(String path) {
        return "polyfactory:" + path;
    }

    @ModifyArg(method = "method_5259", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/DSL;optionalFields([Lcom/mojang/datafixers/util/Pair;)Lcom/mojang/datafixers/types/templates/TypeTemplate;"))
    private static Pair<String, TypeTemplate>[] addCustomComponents(Pair<String, TypeTemplate>[] components,
                                                                    @Local(argsOnly = true) Schema schema) {
        var list = new ArrayList<>(List.of(components));
        list.add(Pair.of("Ingredients", DSL.list(TypeReferences.ITEM_STACK.in(schema))));
        list.add(Pair.of("cached_data", FactoryTypeReferences.DATA_CONTAINER.in(schema)));
        list.add(Pair.of("item", TypeReferences.ITEM_STACK.in(schema)));
        list.add(Pair.of("key1", TypeReferences.ITEM_STACK.in(schema)));
        list.add(Pair.of("key2", TypeReferences.ITEM_STACK.in(schema)));
        return list.toArray(components);
    }
}
