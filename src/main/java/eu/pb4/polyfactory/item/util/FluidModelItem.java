package eu.pb4.polyfactory.item.util;

import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.item.PolymerItem;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;

public class FluidModelItem extends Item implements PolymerItem {
    public <T extends Block & PolymerBlock> FluidModelItem(Properties settings) {
        super(settings);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.PAPER;
    }

    @Override
    public Identifier getPolymerItemModel(ItemStack itemStack, PacketContext context) {
        var x = getFluid(itemStack);
        if (x != null) {
            //noinspection DataFlowIssue
            return FactoryModels.FLUID_FLAT_FULL.getRaw(x).get(DataComponents.ITEM_MODEL);
        }

        return FactoryModels.PLACEHOLDER;
    }

    private FluidInstance<?> getFluid(ItemStack itemStack) {
       return itemStack.getOrDefault(FactoryDataComponents.CURRENT_FLUID, null);
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context) {
        var base = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);

        //noinspection unchecked
        var x = (FluidInstance<Object>) getFluid(itemStack);
        if (x != null && x.type().color().isPresent()) {
            base.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(x.type().color().get().getColor(x.data()))));
        }

        return base;
    }
}
