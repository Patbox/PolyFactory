package eu.pb4.polyfactory.data;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import org.jetbrains.annotations.Nullable;

public record ItemStackData(ItemStack stack, String name) implements DataContainer {

    public static ItemStackData of(ItemStack stack) {
        return new ItemStackData(stack.copy(), stack.getName().getString());
    }

    @Override
    public DataType type() {
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
    public void writeNbt(NbtCompound compound) {
        compound.put("value", this.stack.writeNbt(new NbtCompound()));
        compound.putString("name", this.name);
    }

    public static DataContainer fromNbt(NbtCompound compound) {
        return new ItemStackData(ItemStack.fromNbt(compound.getCompound("value")), compound.getString("name"));
    }

}
