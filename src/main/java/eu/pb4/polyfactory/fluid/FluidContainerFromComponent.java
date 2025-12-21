package eu.pb4.polyfactory.fluid;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.List;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;

public interface FluidContainerFromComponent extends FluidContainer {
    static FluidContainerFromComponent of(ItemStack stack) {
        return new FluidContainerFromComponent() {
            @Override
            public FluidComponent component() {
                return stack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
            }

            @Override
            public void setComponent(FluidComponent component) {
                stack.set(FactoryDataComponents.FLUID, component);
            }
        };
    }

    static FluidContainerFromComponent of(SlotAccess stack) {
        return new FluidContainerFromComponent() {
            @Override
            public FluidComponent component() {
                return stack.get().getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
            }

            @Override
            public void setComponent(FluidComponent component) {
                stack.get().set(FactoryDataComponents.FLUID, component);
            }
        };
    }

    static FluidContainerFromComponent ofCopying(SlotAccess stack) {
        return new FluidContainerFromComponent() {
            @Override
            public FluidComponent component() {
                return stack.get().getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
            }

            @Override
            public void setComponent(FluidComponent component) {
                var copy = stack.get().copy();
                copy.set(FactoryDataComponents.FLUID, component);
                stack.set(copy);
            }
        };
    }

    @Override
    default long get(FluidInstance<?> type) {
        return component().get(type);
    }

    @Override
    default long set(FluidInstance<?> type, long amount) {
        var comp = component();
        var current = comp.get(type);
        setComponent(comp.with(type, amount));
        return current;
    }

    @Override
    default boolean canInsert(FluidInstance<?> type, long amount, boolean exact) {
        var comp = component();
        return exact ? comp.stored() + amount <= comp.capacity() : comp.stored() != comp.capacity();
    }

    @Override
    default long insert(FluidInstance<?> type, long amount, boolean exact) {
        var res = component().insert(type, amount, exact);
        setComponent(res.component());
        return res.fluidAmount();
    }

    @Override
    default boolean canExtract(FluidInstance<?> type, long amount, boolean exact) {
        var comp = component();
        return exact ? comp.get(type) >= amount : comp.get(type) > 0;
    }

    @Override
    default long extract(FluidInstance<?> type, long amount, boolean exact) {
        var res = component().extract(type, amount, exact);
        setComponent(res.component());
        return res.fluidAmount();    }

    @Override
    default long capacity() {
        return component().capacity();
    }

    @Override
    default long stored() {
        return component().stored();
    }

    @Override
    default Object2LongMap<FluidInstance<?>> asMap() {
        return component().map();
    }

    @Override
    default List<FluidInstance<?>> fluids() {
        return component().fluids();
    }

    @Override
    default void clear() {
        setComponent(component().clear());
    }

    FluidComponent component();
    void setComponent(FluidComponent component);
}
