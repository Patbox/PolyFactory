package eu.pb4.polyfactory.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public record ItemStackData(ItemStack stack, String name) implements DataContainer {
    public static MapCodec<ItemStackData> TYPE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.orElse(ItemStack.EMPTY).fieldOf("value").forGetter(ItemStackData::stack),
            Codec.STRING.optionalFieldOf("name", "").forGetter(ItemStackData::name)
    ).apply(instance, ItemStackData::new));


    public static ItemStackData of(ItemStack stack) {
        return new ItemStackData(stack.copy(), stack.getHoverName().getString());
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
    public DataContainer extract(String field) {
        return switch (field) {
            case "type" -> new StringData(BuiltInRegistries.ITEM.getKey(this.stack.getItem()).toString());
            case "name" -> new StringData(asString());
            case "count" -> new LongData(asLong());
            case "damage" -> new LongData(this.stack.getDamageValue());
            default -> DataContainer.super.extract(field);
        };
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ItemStackData that = (ItemStackData) object;
        return Objects.equals(name, that.name) && ItemStack.isSameItemSameComponents(stack, that.stack);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + ItemStack.hashItemAndComponents(stack);
    }

    @Override
    public int compareTo(DataContainer other) {
        return this.name.compareTo(other.asString());
    }
}
