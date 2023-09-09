package eu.pb4.polyfactory.item.util;

import eu.pb4.polyfactory.models.BaseItemProvider;
import net.minecraft.item.Item;

public class ModeledItem extends Item implements SimpleModeledPolymerItem {
    private final Item modelItem;

    public ModeledItem(Item item, Settings settings) {
        super(settings);
        this.modelItem = item;
    }

    public ModeledItem(Settings settings) {
        this(BaseItemProvider.requestItem(), settings);
    }

    @Override
    public Item getPolymerItem() {
        return this.modelItem;
    }
}
