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
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class FluidModelItem extends Item implements PolymerItem {
    public <T extends Block & PolymerBlock> FluidModelItem(Settings settings) {
        super(settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        var x = getFluid(itemStack);
        if (x != null) {
            return FactoryModels.FLUID_FLAT_FULL.getRaw(x).getItem();
        }

        return Items.PAPER;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context) {
        var x = getFluid(itemStack);
        if (x != null) {
            //noinspection DataFlowIssue
            return FactoryModels.FLUID_FLAT_FULL.getRaw(x).get(DataComponentTypes.ITEM_MODEL);
        }

        return FactoryModels.PLACEHOLDER;
    }

    private FluidInstance<?> getFluid(ItemStack itemStack) {
       return itemStack.getOrDefault(FactoryDataComponents.CURRENT_FLUID, null);
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        var base = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);

        //noinspection unchecked
        var x = (FluidInstance<Object>) getFluid(itemStack);
        if (x != null && x.type().color().isPresent()) {
            base.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent((x.type().color().get()).getColor(x.data()), false));
        }

        return base;
    }
}
