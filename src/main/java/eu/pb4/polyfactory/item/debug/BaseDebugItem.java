package eu.pb4.polyfactory.item.debug;

import eu.pb4.factorytools.api.item.ModeledItem;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Rarity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static eu.pb4.polyfactory.ModInit.id;

public class BaseDebugItem extends Item implements PolymerItem {
    private static final PolymerModelData MODEL = PolymerResourcePackUtils.requestModel(Items.WOLF_ARMOR, id("item/debug_item"));
    private final int color;
    private final Text name;

    public BaseDebugItem(String name, int color) {
        super(new Settings().maxCount(1).rarity(Rarity.EPIC));
        this.name = Text.literal("Debug: " + name);
        this.color = color;
    }

    public static BaseDebugItem onBlockInteract(String name, int color, Consumer<ItemUsageContext> consumer) {
        return new BaseDebugItem(name, color) {
            @Override
            public ActionResult useOnBlock(ItemUsageContext context) {
                consumer.accept(context);
                return ActionResult.SUCCESS;
            }
        };
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public Text getName() {
        return this.name;
    }

    @Override
    public Text getName(ItemStack stack) {
        return this.name;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return MODEL.item();
    }

    @Override
    public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return MODEL.value();
    }

    @Override
    public int getPolymerArmorColor(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return this.color;
    }
}
