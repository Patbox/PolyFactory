package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import org.jetbrains.annotations.Nullable;

public record ItemStackData(ItemStack stack, String name) implements DataContainer {
    public static MapCodec<ItemStackData> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.orElse(ItemStack.EMPTY).fieldOf("value").forGetter(ItemStackData::stack),
            Codec.STRING.optionalFieldOf("name", "").forGetter(ItemStackData::name)
    ).apply(instance, ItemStackData::new));


    public static ItemStackData of(ItemStack stack) {
        return new ItemStackData(stack.copy(), stack.getName().getString());
    }

    @Override
    public DataType<ItemStackData> type() {
        return DataType.ITEM_STACK;
    }

    @Override
    public String asString() {
        return name;
    }

    @Override
    public long asLong() {
        return stack.getCount();
    }

    @Override
    public double asDouble() {
        return asLong();
    }

    @Override
    public boolean isEmpty() {
        return this.stack.isEmpty();
    }

    @Override
    public int compareTo(DataContainer other) {
        return this.name.compareTo(other.asString());
    }
}
