package eu.pb4.polyfactory.block.mechanical.source;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public class WindmillBlockEntity extends BlockEntity {
    private DefaultedList<ItemStack> sails = DefaultedList.of();

    public WindmillBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.WINDMILL, pos, state);
        for (int i = 0; i < state.get(WindmillBlock.SAIL_COUNT); i++) {
            sails.add(new ItemStack(FactoryItems.WINDMILL_SAIL));
        }
    }


    @Override
    protected void writeNbt(NbtCompound nbt) {
        var list = new NbtList();
        for (var sail : this.sails) {
            list.add(sail.writeNbt(new NbtCompound()));
        }
        nbt.put("Sails", list);
    }


    @Override
    public void readNbt(NbtCompound nbt) {
        this.sails.clear();
        for (var sail : nbt.getList("Sails", NbtElement.COMPOUND_TYPE)) {
            this.sails.add(ItemStack.fromNbt((NbtCompound) sail));
        }
    }

    public boolean addSail(int i, ItemStack stack) {
        if (stack.isOf(FactoryItems.WINDMILL_SAIL)) {
            if (i < this.sails.size()) {
                this.sails.set(i, stack.copyWithCount(1));
            } else {
                this.sails.add(stack.copyWithCount(1));
            }
            stack.decrement(1);
            this.markDirty();

            var model = BlockBoundAttachment.get(this.world, this.pos);

            if (model != null) {
                ((WindmillBlock.Model) model.holder()).updateSailsBe();
            }

            return true;
        }
        return false;
    }

    public int getSailColor(int i) {
        if (i < this.sails.size()) {
            var sail = this.sails.get(i);

            if (sail.hasNbt() && sail.getNbt().contains("display", NbtElement.COMPOUND_TYPE)) {
                var d = sail.getNbt().getCompound("display");

                if (d.contains("color", NbtElement.NUMBER_TYPE)) {
                    return d.getInt("color");
                }
            }
        }

        return 0xFFFFFF;
    }

    public DefaultedList<ItemStack> getSails() {
        return this.sails;
    }
}
