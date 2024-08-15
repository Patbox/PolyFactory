package eu.pb4.polyfactory.fluid;


import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import net.minecraft.util.Unit;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface FluidBehaviours {
    long EXPERIENCE_ORB_TO_FLUID = 500;
    Map<BlockState, Pair<FluidStack<Unit>, BlockState>> BLOCK_STATE_TO_FLUID_EXTRACT = new IdentityHashMap<>();
    Map<BlockState, List<Pair<FluidStack<?>, BlockState>>> BLOCK_STATE_TO_FLUID_INSERT = new IdentityHashMap<>();

    Map<Item, Function<ItemStack, @Nullable FluidInstance<?>>> ITEM_TO_FLUID = new IdentityHashMap<>();

    static void addItemToFluidLink(Item item, @Nullable FluidInstance<?> instance) {
        ITEM_TO_FLUID.put(item, (x) -> instance);
    }

    static void addItemToFluidLink(Item item, Function<ItemStack, @Nullable FluidInstance<?>> function) {
        ITEM_TO_FLUID.put(item, function);
    }

    static void addBlockStateConversions(BlockState withFluid, BlockState withoutFluid, FluidStack<Unit> fluid) {
        BLOCK_STATE_TO_FLUID_EXTRACT.put(withFluid, new Pair<>(fluid, withoutFluid));
        BLOCK_STATE_TO_FLUID_INSERT.computeIfAbsent(withoutFluid, (a) -> new ArrayList<>()).add(new Pair<>(fluid, withFluid));
    }
    static void addBlockStateInsert(BlockState withFluid, BlockState withoutFluid, FluidStack<Unit> fluid) {
        BLOCK_STATE_TO_FLUID_INSERT.computeIfAbsent(withoutFluid, (a) -> new ArrayList<>()).add(new Pair<>(fluid, withFluid));
    }
}
