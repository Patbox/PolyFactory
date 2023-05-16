package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.item.util.AutoModeledPolymerItem;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class PressTemplateItem extends Item implements AutoModeledPolymerItem {
    public static final Identifier DEFAULT_TYPE = FactoryUtil.id("default");

    public PressTemplateItem(Settings settings) {
        super(settings);
    }

    public static Identifier getType(ItemStack template) {
        return DEFAULT_TYPE;
    }

    @Override
    public void defineModels(Identifier selfId) {

    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return null;
    }
}
