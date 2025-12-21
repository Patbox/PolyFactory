package eu.pb4.polyfactory.item.debug;

import eu.pb4.polymer.core.api.item.PolymerItem;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;

import static eu.pb4.polyfactory.ModInit.id;

public class BaseDebugItem extends Item implements PolymerItem {
    private final int color;
    private final Component name;

    public BaseDebugItem(Properties settings, String name, int color) {
        super(settings.component(DataComponents.ITEM_NAME, Component.literal("Debug: " + name)).stacksTo(1)
                .modelId(id("debug_item"))
                .rarity(Rarity.EPIC));
        this.name = Component.literal("Debug: " + name);
        this.color = color;
    }

    public static BaseDebugItem onBlockInteract(Properties settings, String name, int color, Consumer<UseOnContext> consumer) {
        return new BaseDebugItem(settings, name, color) {
            @Override
            public InteractionResult useOn(UseOnContext context) {
                consumer.accept(context);
                return InteractionResult.SUCCESS_SERVER;
            }
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        return this.name;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TRIAL_KEY;
    }

    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context) {
        out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), IntList.of(this.color)));
    }
}
