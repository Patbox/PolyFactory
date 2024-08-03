package eu.pb4.polyfactory.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipAppender;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public record FluidComponent(Object2LongMap<FluidInstance<?>> map, List<FluidInstance<?>> fluids, long stored, boolean showTooltip) implements TooltipAppender {
    public static final FluidComponent DEFAULT = new FluidComponent(Object2LongMaps.emptyMap(), List.of(), 0, true);
    public static final Codec<FluidComponent> SIMPLE_CODEC = FluidStack.CODEC.listOf().xmap(FluidComponent::fromStacks, FluidComponent::toStacks);
    public static final Codec<FluidComponent> CODEC =  Codec.withAlternative(RecordCodecBuilder.create(instance -> instance.group(
            FluidStack.CODEC.listOf().fieldOf("fluids").forGetter(FluidComponent::toStacks),
            Codec.BOOL.optionalFieldOf("show_tooltip", true).forGetter(FluidComponent::showTooltip)
    ).apply(instance, FluidComponent::fromStacks)), SIMPLE_CODEC);

    private static FluidComponent fromStacks(Collection<FluidStack<?>> fluidStacks) {
        return fromStacks(fluidStacks, true);
    }
    private static FluidComponent fromStacks(Collection<FluidStack<?>> fluidStacks, boolean showTooltip) {
        var map = new Object2LongOpenHashMap<FluidInstance<?>>();
        var list = new ArrayList<FluidInstance<?>>();
        long stored = 0;
        for (var stack : fluidStacks) {
            map.put(stack.instance(), stack.amount());
            list.add(stack.instance());
            stored += stack.amount();
        }

        list.sort(FluidInstance.DENSITY_COMPARATOR_REVERSED);
        return new FluidComponent(map, list, stored, showTooltip);
    }

    public static FluidComponent copyFrom(FluidContainer container) {
        return new FluidComponent(new Object2LongOpenHashMap<>(container.asMap()), new ArrayList<>(container.orderList()), container.stored(), true);
    }

    public List<FluidStack<?>> toStacks() {
        var list = new ArrayList<FluidStack<?>>(this.fluids.size());
        for (var x : fluids) {
            list.add(x.stackOf(map.getOrDefault(x, 0)));
        }
        return list;
    }

    public FluidComponent extractTo(FluidContainer container) {
        var list = new ArrayList<FluidStack<?>>();
        for (var fluid : fluids) {
            var leftover = container.insert(fluid, this.map.getOrDefault(fluid, 0), false);
            if (leftover != 0) {
                list.add(fluid.stackOf(leftover));
            }
        }
        return FluidComponent.fromStacks(list, this.showTooltip);
    }

    public FluidComponent insert(FluidInstance<?> fluid, long amount) {
        return with(fluid, this.map.getOrDefault(fluid, 0) + amount);
    }

    public long get(FluidInstance<?> fluid) {
        return this.map.getOrDefault(fluid, 0);
    }

    public FluidComponent with(FluidInstance<?> fluid, long amount) {
        if (amount == 0 && this.map.containsKey(fluid)) {
            var newMap = new Object2LongOpenHashMap<>(this.map);
            newMap.removeLong(fluid);
            var list = new ArrayList<>(this.fluids);
            list.remove(fluid);
            return new FluidComponent(newMap, list, this.stored - this.map.getOrDefault(fluid, 0), true);
        } else if (amount != this.map.getOrDefault(fluid, 0)) {
            var newMap = new Object2LongOpenHashMap<>(this.map);
            newMap.put(fluid, amount);
            var list = new ArrayList<>(this.fluids);
            if (!this.map.containsKey(fluid)) {
                list.add(fluid);
                list.sort(FluidInstance.DENSITY_COMPARATOR_REVERSED);
            }
            return new FluidComponent(newMap, list, this.stored - this.map.getOrDefault(fluid, 0) + amount, true);
        }
        return this;
    }

    public void copyTo(FluidContainer container) {
        container.clear();
        map.forEach(container::set);
    }


    public FluidComponent withShowTooltip(boolean value) {
        return new FluidComponent(this.map, this.fluids, this.stored, value);
    }

    public boolean isEmpty() {
        return this.stored == 0;
    }

    @Override
    public void appendTooltip(Item.TooltipContext context, Consumer<Text> tooltip, TooltipType type) {
        if (!this.showTooltip || this.isEmpty()) {
            return;
        }
        //tooltip.accept(Text.translatable("text.polyfactory.fluid_component.stored_fluids").formatted(Formatting.GRAY));
        for (var fluid : fluids) {
            tooltip.accept(Text.literal(" ").append(fluid.toLabeledAmount(this.map.getOrDefault(fluid, 0))).formatted(Formatting.GRAY));
        }
    }
}
