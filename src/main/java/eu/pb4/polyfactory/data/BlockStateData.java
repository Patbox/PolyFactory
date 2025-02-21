package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.NotNull;

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

    @Override
    public DataContainer extract(String field) {
        if (field.equals("type")) {
            return new StringData(Registries.BLOCK.getId(this.state.getBlock()));
        }
        if (field.startsWith("property:")) {
            var property = this.state.getBlock().getStateManager().getProperty(field.substring("property:".length()));
            var value = this.state.get(property);
            if (value instanceof Boolean bool) {
                return BoolData.of(bool);
            } else if (value instanceof Integer integer) {
                return new LongData(integer);
            }

            //noinspection unchecked
            return new StringData(((Property) property).name(value));
        }

        return DataContainer.super.extract(field);
    }


    @Override
    public int compareTo(@NotNull DataContainer o) {
        return asString().compareTo(o.asString());
    }
}
