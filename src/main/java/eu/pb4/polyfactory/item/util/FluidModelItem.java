package eu.pb4.polyfactory.item.util;

import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class FluidModelItem extends Item implements PolymerItem {
    public <T extends Block & PolymerBlock> FluidModelItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        var x = getFluid(itemStack);
        if (x != null) {
            return FactoryModels.FLUID_FLAT_FULL.getRaw(x).getItem();
        }

        return FactoryModels.PLACEHOLDER.item();
    }

    @Override
    public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        var x = getFluid(itemStack);
        if (x != null) {
            //noinspection DataFlowIssue
            return FactoryModels.FLUID_FLAT_FULL.getRaw(x).get(DataComponentTypes.CUSTOM_MODEL_DATA).value();
        }

        return FactoryModels.PLACEHOLDER.value();
    }

    private FluidInstance<?> getFluid(ItemStack itemStack) {
       return itemStack.getOrDefault(FactoryDataComponents.CURRENT_FLUID, null);
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, RegistryWrapper.WrapperLookup lookup, @Nullable ServerPlayerEntity player) {
        var base = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, lookup, player);

        //noinspection unchecked
        var x = (FluidInstance<Object>) getFluid(itemStack);
        if (x != null && x.type().color().isPresent()) {
            base.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent((x.type().color().get()).getColor(x.data()), false));
        }

        return base;
    }
}
