package eu.pb4.polyfactory.util.storage.fabricbkp;

import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Optional;

public class PFItemStorage {
    public static final ItemApiLookup<Storage<ItemVariant>, ContainerItemContext> ITEM =
            ItemApiLookup.get(Identifier.of("fabric_backport", "item_storage"), Storage.asClass(), ContainerItemContext.class);


    public static ItemVariant withComponentChanges(ItemVariant variant, ComponentChanges changes) {
        return ItemVariant.of(variant.getItem(), mergeChanges(variant.getComponents(), changes));
    }

    public static ComponentChanges mergeChanges(ComponentChanges base, ComponentChanges applied) {
        ComponentChanges.Builder builder = ComponentChanges.builder();

        writeChangesTo(base, builder);
        writeChangesTo(applied, builder);

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static void writeChangesTo(ComponentChanges changes, ComponentChanges.Builder builder) {
        for (Map.Entry<ComponentType<?>, Optional<?>> entry : changes.entrySet()) {
            if (entry.getValue().isPresent()) {
                builder.add((ComponentType<Object>) entry.getKey(), entry.getValue().get());
            } else {
                builder.remove(entry.getKey());
            }
        }
    }


    static  {
        PFItemStorage.ITEM.registerForItems(
                (itemStack, context) -> new ContainerComponentStorage(context, 27),
                Items.SHULKER_BOX,
                Items.WHITE_SHULKER_BOX,
                Items.ORANGE_SHULKER_BOX,
                Items.MAGENTA_SHULKER_BOX,
                Items.LIGHT_BLUE_SHULKER_BOX,
                Items.YELLOW_SHULKER_BOX,
                Items.LIME_SHULKER_BOX,
                Items.PINK_SHULKER_BOX,
                Items.GRAY_SHULKER_BOX,
                Items.LIGHT_GRAY_SHULKER_BOX,
                Items.CYAN_SHULKER_BOX,
                Items.PURPLE_SHULKER_BOX,
                Items.BLUE_SHULKER_BOX,
                Items.BROWN_SHULKER_BOX,
                Items.GREEN_SHULKER_BOX,
                Items.RED_SHULKER_BOX,
                Items.BLACK_SHULKER_BOX
        );

        PFItemStorage.ITEM.registerForItems(
                (itemStack, context) -> new BundleContentsStorage(context),
                Items.BUNDLE
        );
    }
}
