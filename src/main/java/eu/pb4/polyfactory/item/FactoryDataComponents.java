package eu.pb4.polyfactory.item;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.component.DataComponentType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FactoryDataComponents {
    private static final Codec<ItemStack> UNCOUNTED_ITEM_STACK_WITH_AIR = Codec.withAlternative(ItemStack.UNCOUNTED_CODEC, Codec.unit(ItemStack.EMPTY));

    public static final DataComponentType<Integer> COLOR = register("color", DataComponentType.<Integer>builder().codec(Codec.INT).build());
    public static final DataComponentType<Integer> USES_LEFT = register("uses_left", DataComponentType.<Integer>builder().codec(Codec.INT).build());
    public static final DataComponentType<ItemStack> ITEM_FILTER = register("item_filter", DataComponentType.<ItemStack>builder()
            .codec(UNCOUNTED_ITEM_STACK_WITH_AIR).build());
    public static final DataComponentType<Pair<ItemStack, ItemStack>> REMOTE_KEYS = register("remote_keys", DataComponentType.<Pair<ItemStack, ItemStack>>builder()
            .codec(Codec.pair(UNCOUNTED_ITEM_STACK_WITH_AIR.fieldOf("1").codec(), UNCOUNTED_ITEM_STACK_WITH_AIR.fieldOf("2").codec())).build());

    public static final DataComponentType<DataContainer> STORED_DATA = register("stored_data", DataComponentType.<DataContainer>builder()
            .codec(DataContainer.CODEC.codec()).build());

    public static final DataComponentType<Boolean> READ_ONLY = register("read_only", DataComponentType.<Boolean>builder().codec(Codec.BOOL).build());

    public static void register() {

    }


    public static <T> DataComponentType<T> register(String path, DataComponentType<T> item) {
        Registry.register(Registries.DATA_COMPONENT_TYPE, new Identifier(ModInit.ID, path), item);
        PolymerItemUtils.markAsPolymer(item);
        return item;
    }
}
