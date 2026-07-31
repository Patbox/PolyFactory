package eu.pb4.polyfactory.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.polyfactory.fluid.*;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.fluid.HorizontalFluidTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import org.jetbrains.annotations.Nullable;

public record FluidComponent(Object2LongMap<FluidInstance<?>> map, List<FluidInstance<?>> fluids,
                             long stored, long capacity) implements TooltipProvider, FluidHolder {
    public static final FluidComponent DEFAULT = new FluidComponent(Object2LongMaps.emptyMap(), List.of(), 0, -1);
    public static final Codec<FluidComponent> SIMPLE_CODEC = FluidStack.CODEC.listOf().xmap(FluidComponent::fromStacks, FluidComponent::toStacks);
    public static final Codec<FluidComponent> CODEC =  Codec.withAlternative(RecordCodecBuilder.create(instance -> instance.group(
            FluidStack.CODEC.listOf().fieldOf("fluids").forGetter(FluidComponent::toStacks),
            Codec.LONG.optionalFieldOf("capacity", -1L).forGetter(FluidComponent::capacity)
    ).apply(instance, FluidComponent::fromStacks)), SIMPLE_CODEC);

    private static FluidComponent fromStacks(Collection<FluidStack<?>> fluidStacks) {
        return fromStacks(fluidStacks, -1);
    }
    private static FluidComponent fromStacks(Collection<FluidStack<?>> fluidStacks, long capacity) {
        var map = new Object2LongOpenHashMap<FluidInstance<?>>();
        var list = new ArrayList<FluidInstance<?>>();
        long stored = 0;
        for (var stack : fluidStacks) {
            map.put(stack.instance(), stack.amount());
            list.add(stack.instance());
            stored += stack.amount();
        }

        list.sort(FluidInstance.DENSITY_COMPARATOR_REVERSED);
        return new FluidComponent(map, list, stored, capacity);
    }

    public static FluidComponent copyFrom(FluidContainer container) {
        return container.asFluidComponent();
    }

    public static FluidComponent empty(long capacity) {
        return new FluidComponent(Object2LongMaps.emptyMap(), List.of(), 0, capacity);
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
        return this.isInfinite() ? this : FluidComponent.fromStacks(list, this.capacity);
    }

    public boolean isInfinite() {
        return this.capacity == Long.MAX_VALUE;
    }

    public Result insert(FluidInstance<?> fluid, long amount, boolean strict) {
        if (this.capacity == -1) {
            return new Result(with(fluid, this.get(fluid) + amount), 0);
        }
        if (strict && this.stored + amount > this.capacity) {
            return new Result(this, amount);
        }

        var maxAmount = Math.min(this.capacity - this.stored, amount);
        if (maxAmount == 0) {
            return new Result(this, amount);
        }

        return new Result(this.isInfinite() ? this : with(fluid, this.get(fluid) + maxAmount), amount - maxAmount);
    }

    public Result extract(FluidInstance<?> fluid, long amount, boolean strict) {
        if (strict && get(fluid) < amount) {
            return new Result(this, 0);
        }

        var maxAmount = Math.min(get(fluid), amount);
        if (maxAmount == 0) {
            return new Result(this, 0);
        }
        return new Result(this.isInfinite() ? this : with(fluid, this.get(fluid) - maxAmount), maxAmount);
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
            return new FluidComponent(newMap, list, this.stored - this.map.getOrDefault(fluid, 0), this.capacity);
        } else if (amount != this.map.getOrDefault(fluid, 0)) {
            var newMap = new Object2LongOpenHashMap<>(this.map);
            newMap.put(fluid, amount);
            var list = new ArrayList<>(this.fluids);
            if (!this.map.containsKey(fluid)) {
                list.add(fluid);
                list.sort(FluidInstance.DENSITY_COMPARATOR_REVERSED);
            }
            return new FluidComponent(newMap, list, this.stored - this.map.getOrDefault(fluid, 0) + amount, this.capacity);
        }
        return this;
    }

    public void copyTo(FluidContainer container) {
        container.clear();
        map.forEach(container::set);
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag type, DataComponentGetter components) {
        for (var fluid : fluids.reversed()) {
            tooltip.accept(Component.literal(" ").append(fluid.toLabeledAmount(this.map.getOrDefault(fluid, 0))).withStyle(ChatFormatting.GRAY));
        }

        if (this.capacity != -1) {
            tooltip.accept(Component.translatable("text.polyfactory.x_out_of_y", FactoryUtil.fluidTextGeneric(this.stored), FactoryUtil.fluidTextGeneric(this.capacity)).withStyle(ChatFormatting.YELLOW));
        }

        if (PolymerCommonUtils.isServerNetworkingThread()) {
            tooltip.accept(Component.literal(" ").append(HorizontalFluidTextures.TOOLTIP.render(this::provideRender))
                    .append(GuiTextures.negativeSpace(1))
                    .append(GuiTextures.FLUID_TOOLTIP_BACKGROUND).setStyle(Style.EMPTY.withShadowColor(0)));
            tooltip.accept(Component.empty());
        }
    }

    public FluidComponent clear() {
        return new FluidComponent(Object2LongMaps.emptyMap(), List.of(), 0, this.capacity);
    }

    public boolean contains(TagKey<FluidType<?>> tag) {
        for (var x : this.fluids) {
            if (x.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public FluidComponent withCapacity(long capacity) {
        return new FluidComponent(this.map, this.fluids, this.stored, capacity);
    }

    public FluidComponent rotateFluids(boolean previous) {
        if (this.fluids.size() <= 1) {
            return this;
        }

        var list = new ArrayList<>(this.fluids);

        if (previous) {
            list.addFirst(list.removeLast());
        } else {
            list.add(list.removeFirst());
        }

        return new FluidComponent(this.map, list, this.stored, this.capacity);
    }

    public record Result(FluidComponent component, long fluidAmount) {}
}
