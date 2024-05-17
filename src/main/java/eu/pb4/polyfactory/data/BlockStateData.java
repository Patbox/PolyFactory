package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;

public record BlockStateData(BlockState state) implements DataContainer {
    public static MapCodec<BlockStateData> TYPE_CODEC = BlockState.CODEC.xmap(BlockStateData::new, BlockStateData::state).fieldOf("value");


    @Override
    public DataType<BlockStateData> type() {
        return DataType.BLOCK_STATE;
    }

    @Override
    public String asString() {
        return state.getBlock().getName().getString();
    }

    @Override
    public long asLong() {
        return state.isAir() ? 0 : state.getBlock().getStateManager().getStates().indexOf(state) + 1;
    }

    @Override
    public double asDouble() {
        return asLong();
    }

    @Override
    public boolean isEmpty() {
        return state.isAir();
    }
}
