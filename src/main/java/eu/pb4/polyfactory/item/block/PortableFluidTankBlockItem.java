package eu.pb4.polyfactory.item.block;

import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class PortableFluidTankBlockItem extends FactoryBlockItem {
    public <T extends Block & PolymerBlock> PortableFluidTankBlockItem(T block, Settings settings) {
        super(block, settings);
    }


    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, RegistryWrapper.WrapperLookup lookup, @Nullable ServerPlayerEntity player) {
        var base = super.getPolymerItemStack(itemStack, tooltipType, lookup, player);
        if (itemStack.contains(FactoryDataComponents.FLUID)) {
            var fluids = itemStack.get(FactoryDataComponents.FLUID);
            if (fluids != null && fluids.capacity() != -1) {
                base.set(DataComponentTypes.MAX_DAMAGE, (int) (fluids.capacity() / 100));
                base.set(DataComponentTypes.DAMAGE, (int) ((fluids.capacity() - fluids.stored()) / 100));
            }
        }
        return base;
    }
}
