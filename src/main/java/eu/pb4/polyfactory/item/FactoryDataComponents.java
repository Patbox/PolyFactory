package eu.pb4.polyfactory.item;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.component.DataComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FactoryDataComponents {
    private static final Codec<ItemStack> UNCOUNTED_ITEM_STACK_WITH_AIR = ItemStack.UNCOUNTED_CODEC.orElse(ItemStack.EMPTY);

    public static final DataComponentType<Integer> COLOR = register("color", DataComponentType.<Integer>builder().codec(Codec.INT).build());
    public static final DataComponentType<Integer> USES_LEFT = register("uses_left", DataComponentType.<Integer>builder().codec(Codec.INT).build());
    public static final DataComponentType<ItemStack> ITEM_FILTER = register("item_filter", DataComponentType.<ItemStack>builder()
            .codec(UNCOUNTED_ITEM_STACK_WITH_AIR).build());
    public static final DataComponentType<Pair<ItemStack, ItemStack>> REMOTE_KEYS = register("remote_keys", DataComponentType.<Pair<ItemStack, ItemStack>>builder()
            .codec(RecordCodecBuilder.create(instance -> instance.group(
                    UNCOUNTED_ITEM_STACK_WITH_AIR.optionalFieldOf("1", ItemStack.EMPTY).forGetter(Pair::getFirst),
                    UNCOUNTED_ITEM_STACK_WITH_AIR.optionalFieldOf("2", ItemStack.EMPTY).forGetter(Pair::getSecond)
            ).apply(instance, Pair::new))).build());

    public static final DataComponentType<DataContainer> STORED_DATA = register("stored_data", DataComponentType.<DataContainer>builder()
            .codec(DataContainer.MAP_CODEC.codec()).build());

    public static final DataComponentType<Boolean> READ_ONLY = register("read_only", DataComponentType.<Boolean>builder().codec(Codec.BOOL).build());

    public static final DataComponentType<Integer> CHANNEL = register("channel", DataComponentType.<Integer>builder().codec(Codec.INT).build());


    public static void register() {

    }


    public static <T> DataComponentType<T> register(String path, DataComponentType<T> item) {
        Registry.register(Registries.DATA_COMPONENT_TYPE, new Identifier(ModInit.ID, path), item);
        PolymerItemUtils.markAsPolymer(item);
        return item;
    }
}
