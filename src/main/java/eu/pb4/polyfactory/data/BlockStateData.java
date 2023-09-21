package eu.pb4.polyfactory.data;

import eu.pb4.polyfactory.util.StateNameProvider;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;

public record BlockStateData(BlockState state) implements DataContainer {

    @Override
    public DataType type() {
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
    public void writeNbt(NbtCompound compound) {
        compound.put("value", NbtHelper.fromBlockState(this.state));
    }

    public static DataContainer fromNbt(NbtCompound compound) {
        return new BlockStateData(NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), compound.getCompound("value")));
    }

}
