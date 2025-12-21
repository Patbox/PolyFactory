package eu.pb4.polyfactory.item.tool;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polyfactory.fluid.FluidInteractionMode;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class UniversalFluidContainerItem extends SimplePolymerItem {
    private final long capacity;

    public UniversalFluidContainerItem(long capacity, Properties settings) {
        super(settings.component(FactoryDataComponents.FLUID, FluidComponent.DEFAULT)
                .component(FactoryDataComponents.FLUID_INTERACTION_MODE, FluidInteractionMode.EXTRACT));
        this.capacity = capacity;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        tooltip.accept(Component.literal(stack.getOrDefault(FactoryDataComponents.FLUID_INTERACTION_MODE, FluidInteractionMode.EXTRACT).name()));
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack otherStack, Slot slot, ClickAction clickType, Player player, SlotAccess cursorStackReference) {
        if (clickType == ClickAction.SECONDARY) {
            stack.set(FactoryDataComponents.FLUID_INTERACTION_MODE, stack.get(FactoryDataComponents.FLUID_INTERACTION_MODE) == FluidInteractionMode.EXTRACT ? FluidInteractionMode.INSERT : FluidInteractionMode.EXTRACT);

            return true;
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction clickType, Player player) {
        if (clickType == ClickAction.SECONDARY) {
            stack.set(FactoryDataComponents.FLUID_INTERACTION_MODE, stack.get(FactoryDataComponents.FLUID_INTERACTION_MODE) == FluidInteractionMode.EXTRACT ? FluidInteractionMode.INSERT : FluidInteractionMode.EXTRACT);

            return true;
        }
        return false;
    }

    public long capacity() {
        return capacity;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipFlag tooltipType, PacketContext context) {
        var out = super.getPolymerItemStack(itemStack, tooltipType, context);
        var fluids = itemStack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
        out.set(DataComponents.MAX_DAMAGE, 1000);
        out.set(DataComponents.DAMAGE, 1000 - (int) ((fluids.stored() / (double) this.capacity) * 999));
        return out;
    }
}
