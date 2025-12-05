package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.block.data.io.DataMemoryBlock;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ClickType;

public class DataMemoryBlockItem extends FactoryBlockItem {
    public <T extends Block & PolymerBlock> DataMemoryBlockItem(T block, Settings settings) {
        super(block, settings);
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (stack.contains(FactoryDataComponents.STORED_DATA) && clickType == ClickType.RIGHT && otherStack.isOf(FactoryItems.WRENCH)) {
            stack.set(FactoryDataComponents.READ_ONLY, !stack.getOrDefault(FactoryDataComponents.READ_ONLY, false));
            FactoryUtil.playSoundToPlayer(player,SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.UI, 0.5f, 1f);
            return true;
        }
        return false;
    }
}
