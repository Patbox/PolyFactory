package eu.pb4.polyfactory.item.debug;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.function.Consumer;

import static eu.pb4.polyfactory.ModInit.id;

public class BaseDebugItem extends Item implements PolymerItem {
    private static final Identifier MODEL = id("debug_item");
    private final int color;

    public BaseDebugItem(Settings settings, String name, int color) {
        super(settings.component(DataComponentTypes.ITEM_NAME, Text.literal("Debug: " + name)).maxCount(1).rarity(Rarity.EPIC));
        this.color = color;
    }

    public static BaseDebugItem onBlockInteract(Settings settings, String name, int color, Consumer<ItemUsageContext> consumer) {
        return new BaseDebugItem(settings, name, color) {
            @Override
            public ActionResult useOnBlock(ItemUsageContext context) {
                consumer.accept(context);
                return ActionResult.SUCCESS_SERVER;
            }
        };
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.WOLF_ARMOR;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return MODEL;
    }

    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context) {
        out.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(this.color, false));
    }
}
