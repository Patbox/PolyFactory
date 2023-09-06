package eu.pb4.polyfactory.item.util;

import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;

public interface SimpleModeledPolymerItem extends AutoModeledPolymerItem {
    IdentityHashMap<Object, PolymerModelData> MODELS = new IdentityHashMap<>();


    Item getPolymerItem();

    @Override
    default Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return this.getPolymerItem();
    }

    @Override
    default int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return MODELS.get(this).value();
    }

    default int getPolymerCustomModelData() {
        return MODELS.get(this).value();
    }

    @Override
    default void defineModels(Identifier selfId) {
        var item = new Identifier(selfId.getNamespace(), "item/" + selfId.getPath());
        MODELS.put(this, PolymerResourcePackUtils.requestModel(this.getPolymerItem(), item));
    }
}
