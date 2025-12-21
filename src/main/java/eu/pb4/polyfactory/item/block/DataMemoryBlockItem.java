package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.block.data.io.DataMemoryBlock;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class DataMemoryBlockItem extends FactoryBlockItem {
    public <T extends Block & PolymerBlock> DataMemoryBlockItem(T block, Properties settings) {
        super(block, settings);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, Player player, SlotAccess cursorStackReference) {
        if (stack.has(FactoryDataComponents.STORED_DATA) && clickType == ClickAction.SECONDARY && otherStack.is(FactoryItems.WRENCH)) {
            stack.set(FactoryDataComponents.READ_ONLY, !stack.getOrDefault(FactoryDataComponents.READ_ONLY, false));
            FactoryUtil.playSoundToPlayer(player,SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.UI, 0.5f, 1f);
            return true;
        }
        return false;
    }
}
