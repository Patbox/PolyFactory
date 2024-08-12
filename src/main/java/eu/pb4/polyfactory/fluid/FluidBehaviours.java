package eu.pb4.polyfactory.fluid;


import net.minecraft.block.BlockState;
import net.minecraft.util.Pair;
import net.minecraft.util.Unit;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public interface FluidBehaviours {
    long EXPERIENCE_ORB_TO_FLUID = 500;
    Map<BlockState, Pair<FluidStack<Unit>, BlockState>> BLOCK_STATE_TO_FLUID_EXTRACT = new IdentityHashMap<>();
    Map<BlockState, List<Pair<FluidStack<?>, BlockState>>> BLOCK_STATE_TO_FLUID_INSERT = new IdentityHashMap<>();

    static void addBlockStateConversions(BlockState withFluid, BlockState withoutFluid, FluidStack<Unit> fluid) {
        BLOCK_STATE_TO_FLUID_EXTRACT.put(withFluid, new Pair<>(fluid, withoutFluid));
        BLOCK_STATE_TO_FLUID_INSERT.computeIfAbsent(withoutFluid, (a) -> new ArrayList<>()).add(new Pair<>(fluid, withFluid));
    }
    static void addBlockStateInsert(BlockState withFluid, BlockState withoutFluid, FluidStack<Unit> fluid) {
        BLOCK_STATE_TO_FLUID_INSERT.computeIfAbsent(withoutFluid, (a) -> new ArrayList<>()).add(new Pair<>(fluid, withFluid));
    }
}
