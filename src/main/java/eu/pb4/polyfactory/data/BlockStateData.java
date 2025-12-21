package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
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
        return state.isAir() ? 0 : state.getBlock().getStateDefinition().getPossibleStates().indexOf(state) + 1;
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
            return new StringData(BuiltInRegistries.BLOCK.getKey(this.state.getBlock()));
        }
        if (field.startsWith("property:")) {
            var property = this.state.getBlock().getStateDefinition().getProperty(field.substring("property:".length()));
            var value = this.state.getValue(property);
            if (value instanceof Boolean bool) {
                return BoolData.of(bool);
            } else if (value instanceof Integer integer) {
                return new LongData(integer);
            }

            //noinspection unchecked
            return new StringData(((Property) property).getName(value));
        }

        return DataContainer.super.extract(field);
    }


    @Override
    public int compareTo(@NotNull DataContainer o) {
        return asString().compareTo(o.asString());
    }
}
