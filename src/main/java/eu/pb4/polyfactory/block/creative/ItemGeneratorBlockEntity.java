package eu.pb4.polyfactory.block.creative;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.util.inventory.SingleStackInventory;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class ItemGeneratorBlockEntity extends BlockEntity implements SingleStackInventory {
    private ItemStack stack = ItemStack.EMPTY;
    public final SidedInventory infinite = new InfiniteInventory();

    public ItemGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.ITEM_GENERATOR, pos, state);
    }

    @Override
    public ItemStack getStack() {
        return this.stack;
    }

    @Override
    public void setStack(ItemStack stack) {
        this.stack = stack;
    }

    public void openGui(ServerPlayerEntity player) {
        new Gui(player);
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.HOPPER, player, false);
            this.setTitle(ItemGeneratorBlockEntity.this.getCachedState().getBlock().getName());
            var x= new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).setName(Text.empty());
            this.setSlot(0, x);
            this.setSlot(1, x);
            this.setSlot(3, x);
            this.setSlot(4, x);
            this.setSlotRedirect(2, new Slot(ItemGeneratorBlockEntity.this, -1, 0, 0));

            this.open();
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(ItemGeneratorBlockEntity.this.pos)) > (18*18)) {
                this.close();
            }
            super.onTick();
        }
    }

    private class InfiniteInventory implements SidedInventory, SingleStackInventory {
        @Override
        public ItemStack getStack() {
            return ItemGeneratorBlockEntity.this.getStack().copy();
        }

        @Override
        public void setStack(ItemStack stack) {

        }

        @Override
        public int[] getAvailableSlots(Direction side) {
            return new int[] { 0 };
        }

        @Override
        public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
            return false;
        }

        @Override
        public boolean canExtract(int slot, ItemStack stack, Direction dir) {
            return true;
        }
    }
}


