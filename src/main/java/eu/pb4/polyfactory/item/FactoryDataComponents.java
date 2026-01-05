package eu.pb4.polyfactory.item;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidInteractionMode;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.item.configuration.ConfigurationData;
import eu.pb4.polyfactory.item.tool.ImprovedFilterItem;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.fabricmc.fabric.api.item.v1.ComponentTooltipAppenderRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import java.util.List;

import static eu.pb4.polyfactory.ModInit.id;

public class FactoryDataComponents {
    private static final Codec<ItemStack> UNCOUNTED_ITEM_STACK_WITH_AIR = ItemStack.SINGLE_ITEM_CODEC.orElse(ItemStack.EMPTY);

    public static final DataComponentType<Integer> COLOR = register("color", DataComponentType.<Integer>builder().persistent(ExtraCodecs.RGB_COLOR_CODEC).build());
    public static final DataComponentType<Integer> USES_LEFT = register("uses_left", DataComponentType.<Integer>builder().persistent(Codec.INT).build());
    public static final DataComponentType<List<ItemStack>> ITEM_FILTER = register("item_filter", DataComponentType.<List<ItemStack>>builder()
            .persistent(ExtraCodecs.compactListCodec(UNCOUNTED_ITEM_STACK_WITH_AIR)).build());

    public static final DataComponentType<Pair<ItemStack, ItemStack>> REMOTE_KEYS = register("remote_keys", DataComponentType.<Pair<ItemStack, ItemStack>>builder()
            .persistent(RecordCodecBuilder.create(instance -> instance.group(
                    UNCOUNTED_ITEM_STACK_WITH_AIR.optionalFieldOf("1", ItemStack.EMPTY).forGetter(Pair::getFirst),
                    UNCOUNTED_ITEM_STACK_WITH_AIR.optionalFieldOf("2", ItemStack.EMPTY).forGetter(Pair::getSecond)
            ).apply(instance, Pair::new))).build());

    public static final DataComponentType<DataContainer> STORED_DATA = register("stored_data", DataComponentType.<DataContainer>builder()
            .persistent(DataContainer.MAP_CODEC.codec()).build());

    public static final DataComponentType<Boolean> READ_ONLY = register("read_only", DataComponentType.<Boolean>builder().persistent(Codec.BOOL).build());

    public static final DataComponentType<Integer> CHANNEL = register("channel", DataComponentType.<Integer>builder().persistent(Codec.INT).build());
    public static final DataComponentType<FluidComponent> FLUID = register("fluid", DataComponentType.<FluidComponent>builder().persistent(FluidComponent.CODEC).build());
    public static final DataComponentType<FluidInteractionMode> FLUID_INTERACTION_MODE = register("fluid_interaction_mode", DataComponentType.<FluidInteractionMode>builder().persistent(FluidInteractionMode.CODEC).build());
    public static final DataComponentType<FluidInstance<?>> CURRENT_FLUID = register("current_fluid", DataComponentType.<FluidInstance<?>>builder().persistent(FluidInstance.CODEC).build());

    public static final DataComponentType<ImprovedFilterItem.Match> ITEM_FILTER_MATCH = register("item_filter/match", DataComponentType.<ImprovedFilterItem.Match>builder()
            .persistent(StringRepresentable.fromEnum(ImprovedFilterItem.Match::values)).build());

    public static final DataComponentType<ImprovedFilterItem.Type> ITEM_FILTER_TYPE = register("item_filter/type", DataComponentType.<ImprovedFilterItem.Type>builder()
            .persistent(StringRepresentable.fromEnum(ImprovedFilterItem.Type::values)).build());

    public static final DataComponentType<ConfigurationData> CONFIGURATION_DATA = register("configuration_data", DataComponentType.<ConfigurationData>builder().persistent(ConfigurationData.CODEC).build());
    public static final DataComponentType<List<String>> PUNCH_CARD_DATA = register("punch_card_data", DataComponentType.<List<String>>builder().persistent(Codec.STRING.listOf()).build());


    public static void register() {
        ComponentTooltipAppenderRegistry.addFirst(FLUID);
        ComponentTooltipAppenderRegistry.addFirst(STORED_DATA);
        ComponentTooltipAppenderRegistry.addFirst(CONFIGURATION_DATA);
        BuiltInRegistries.DATA_COMPONENT_TYPE.addAlias(id("clipboard_data"), id("configuration_data"));
    }


    public static <T> DataComponentType<T> register(String path, DataComponentType<T> item) {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Identifier.fromNamespaceAndPath(ModInit.ID, path), item);
        PolymerComponent.registerDataComponent(item);
        return item;
    }
}
