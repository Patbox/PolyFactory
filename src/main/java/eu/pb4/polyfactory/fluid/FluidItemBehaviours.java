package eu.pb4.polyfactory.fluid;


import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.ComponentPredicate;
import net.minecraft.sound.SoundEvent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public interface FluidItemBehaviours {
    Map<Item, List<Result>> FLUID_INSERT = new IdentityHashMap<>();
    Map<Item, Map<FluidType, List<Result>>> FLUID_EXTRACT = new IdentityHashMap<>();

    static void addStaticRelation(Item itemWithFluid, Item itemWithoutFluid, FluidStack fluid, SoundEvent insert, SoundEvent extract) {
        addStaticContainer(itemWithoutFluid, fluid, itemWithFluid.getDefaultStack(), extract);
        addStaticSource(itemWithFluid, fluid, itemWithoutFluid.getDefaultStack(), insert);
    }

    static void addStaticRelation(Item itemWithFluid, ComponentPredicate itemWithFluidPredicate, Item itemWithoutFluid, FluidStack fluid, SoundEvent insert, SoundEvent extract) {
        var x = itemWithFluid.getDefaultStack();
        x.applyChanges(itemWithFluidPredicate.toChanges());
        addStaticContainer(itemWithoutFluid, fluid, x, extract);
        addStaticSource(itemWithFluid, itemWithFluidPredicate, fluid, itemWithoutFluid.getDefaultStack(), insert);
    }

    static void addStaticRelation(Item itemWithFluid, ComponentPredicate itemWithFluidPredicate, Item itemWithoutFluid, ComponentPredicate itemWithoutFluidPredicate, FluidStack fluid, SoundEvent insert, SoundEvent extract) {
        var x = itemWithFluid.getDefaultStack();
        x.applyChanges(itemWithFluidPredicate.toChanges());
        var y = itemWithoutFluid.getDefaultStack();
        y.applyChanges(itemWithoutFluidPredicate.toChanges());
        addStaticContainer(itemWithoutFluid, itemWithoutFluidPredicate, fluid, x, extract);
        addStaticSource(itemWithFluid, itemWithFluidPredicate, fluid, y, insert);
    }

    static void addStaticSource(Item item, FluidStack fluid, ItemStack remainder, SoundEvent sound) {
        addStaticSource(item, ComponentPredicate.EMPTY, fluid, remainder, sound);
    }
    static void addStaticSource(Item item, ComponentPredicate predicate, FluidStack fluid, ItemStack remainder, SoundEvent sound) {
        FLUID_INSERT.computeIfAbsent(item, (x) -> new ArrayList<>()).add(new Result(predicate, fluid, remainder, sound));
    }

    static void addStaticContainer(Item item, FluidStack fluid, ItemStack remainder, SoundEvent sound) {
        addStaticContainer(item, ComponentPredicate.EMPTY, fluid, remainder, sound);
    }
    static void addStaticContainer(Item item, ComponentPredicate predicate, FluidStack fluid, ItemStack remainder, SoundEvent sound) {
        FLUID_EXTRACT.computeIfAbsent(item, (x) -> new IdentityHashMap<>()).computeIfAbsent(fluid.type(), (x) -> new ArrayList<>()).add(new Result(predicate, fluid, remainder, sound));
    }


    record Result(ComponentPredicate predicate, FluidStack fluids, ItemStack result, SoundEvent sound) {};
}
