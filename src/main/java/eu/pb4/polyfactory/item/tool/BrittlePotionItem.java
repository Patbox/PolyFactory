package eu.pb4.polyfactory.item.tool;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public class BrittlePotionItem extends Item implements PolymerItem {
    public BrittlePotionItem(Properties settings) {
        super(settings);
    }


    public ItemStack getDefaultInstance() {
        ItemStack itemStack = super.getDefaultInstance();
        itemStack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        return itemStack;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(),
                stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getName(Items.POTION.getDescriptionId() + ".effect."));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TRIAL_KEY;
    }
}
