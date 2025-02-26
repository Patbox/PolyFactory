package eu.pb4.polyfactory.item;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidInteractionMode;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.item.tool.ImprovedFilterItem;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.component.ComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.dynamic.Codecs;

import java.util.List;

public class FactoryDataComponents {
    private static final Codec<ItemStack> UNCOUNTED_ITEM_STACK_WITH_AIR = ItemStack.UNCOUNTED_CODEC.orElse(ItemStack.EMPTY);

    public static final ComponentType<Integer> COLOR = register("color", ComponentType.<Integer>builder().codec(Codec.INT).build());
    public static final ComponentType<Integer> USES_LEFT = register("uses_left", ComponentType.<Integer>builder().codec(Codec.INT).build());
    public static final ComponentType<List<ItemStack>> ITEM_FILTER = register("item_filter", ComponentType.<List<ItemStack>>builder()
            .codec(Codecs.listOrSingle(UNCOUNTED_ITEM_STACK_WITH_AIR)).build());

    public static final ComponentType<Pair<ItemStack, ItemStack>> REMOTE_KEYS = register("remote_keys", ComponentType.<Pair<ItemStack, ItemStack>>builder()
            .codec(RecordCodecBuilder.create(instance -> instance.group(
                    UNCOUNTED_ITEM_STACK_WITH_AIR.optionalFieldOf("1", ItemStack.EMPTY).forGetter(Pair::getFirst),
                    UNCOUNTED_ITEM_STACK_WITH_AIR.optionalFieldOf("2", ItemStack.EMPTY).forGetter(Pair::getSecond)
            ).apply(instance, Pair::new))).build());

    public static final ComponentType<DataContainer> STORED_DATA = register("stored_data", ComponentType.<DataContainer>builder()
            .codec(DataContainer.MAP_CODEC.codec()).build());

    public static final ComponentType<Boolean> READ_ONLY = register("read_only", ComponentType.<Boolean>builder().codec(Codec.BOOL).build());

    public static final ComponentType<Integer> CHANNEL = register("channel", ComponentType.<Integer>builder().codec(Codec.INT).build());
    public static final ComponentType<FluidComponent> FLUID = register("fluid", ComponentType.<FluidComponent>builder().codec(FluidComponent.CODEC).build());
    public static final ComponentType<FluidInteractionMode> FLUID_INTERACTION_MODE = register("fluid_interaction_mode", ComponentType.<FluidInteractionMode>builder().codec(FluidInteractionMode.CODEC).build());
    public static final ComponentType<FluidInstance<?>> CURRENT_FLUID = register("current_fluid", ComponentType.<FluidInstance<?>>builder().codec(FluidInstance.CODEC).build());

    public static final ComponentType<ImprovedFilterItem.Match> ITEM_FILTER_MATCH = register("item_filter/match", ComponentType.<ImprovedFilterItem.Match>builder()
            .codec(StringIdentifiable.createCodec(ImprovedFilterItem.Match::values)).build());

    public static final ComponentType<ImprovedFilterItem.Type> ITEM_FILTER_TYPE = register("item_filter/type", ComponentType.<ImprovedFilterItem.Type>builder()
            .codec(StringIdentifiable.createCodec(ImprovedFilterItem.Type::values)).build());

    public static void register() {

    }


    public static <T> ComponentType<T> register(String path, ComponentType<T> item) {
        Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(ModInit.ID, path), item);
        PolymerComponent.registerDataComponent(item);
        return item;
    }
}
