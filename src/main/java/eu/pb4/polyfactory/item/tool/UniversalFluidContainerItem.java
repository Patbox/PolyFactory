package eu.pb4.polyfactory.item.tool;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polyfactory.fluid.FluidInteractionMode;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;

public class UniversalFluidContainerItem extends SimplePolymerItem {
    private final long capacity;

    public UniversalFluidContainerItem(long capacity, Settings settings) {
        super(settings.component(FactoryDataComponents.FLUID, FluidComponent.DEFAULT)
                .component(FactoryDataComponents.FLUID_INTERACTION_MODE, FluidInteractionMode.EXTRACT));
        this.capacity = capacity;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.literal(stack.getOrDefault(FactoryDataComponents.FLUID_INTERACTION_MODE, FluidInteractionMode.EXTRACT).name()));
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType == ClickType.RIGHT) {
            stack.set(FactoryDataComponents.FLUID_INTERACTION_MODE, stack.get(FactoryDataComponents.FLUID_INTERACTION_MODE) == FluidInteractionMode.EXTRACT ? FluidInteractionMode.INSERT : FluidInteractionMode.EXTRACT);

            return true;
        }
        return false;
    }

    @Override
    public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType == ClickType.RIGHT) {
            stack.set(FactoryDataComponents.FLUID_INTERACTION_MODE, stack.get(FactoryDataComponents.FLUID_INTERACTION_MODE) == FluidInteractionMode.EXTRACT ? FluidInteractionMode.INSERT : FluidInteractionMode.EXTRACT);

            return true;
        }
        return false;
    }

    public long capacity() {
        return capacity;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        var out = super.getPolymerItemStack(itemStack, tooltipType, context);
        var fluids = itemStack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
        out.set(DataComponentTypes.MAX_DAMAGE, 1000);
        out.set(DataComponentTypes.DAMAGE, 1000 - (int) ((fluids.stored() / (double) this.capacity) * 999));
        return out;
    }
}
