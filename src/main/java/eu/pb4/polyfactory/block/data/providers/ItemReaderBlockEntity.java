package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.StringData;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.SingleStackInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class ItemReaderBlockEntity extends ChanneledDataBlockEntity implements SingleStackInventory, ChanneledDataCache, BlockEntityExtraListener {
    private static final String[] NO_DATA = new String[] { "" };
    private ItemStack stack = ItemStack.EMPTY;
    private int page = 0;
    private String[] lines = NO_DATA;
    private ItemReaderBlock.Model model;

    public ItemReaderBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(FactoryBlockEntities.ITEM_READER, blockPos, blockState);
    }

    @Override
    public ItemStack getStack() {
        return this.stack;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.stack = ItemStack.fromNbt(nbt.getCompound("stack"));
        this.page = nbt.getInt("page");
        forceUpdate();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.put("stack", this.stack.writeNbt(new NbtCompound()));
        nbt.putInt("page", this.page);
    }

    @Override
    public void setStack(ItemStack stack) {
        this.stack = stack;
        this.page = 0;
        this.forceUpdate();
        this.markDirty();
    }

    private void forceUpdate() {
        if (this.model != null) {
            this.model.setItem(this.stack);
        }

        if (this.stack.isOf(Items.WRITABLE_BOOK) && this.stack.hasNbt()) {
            var lines = this.stack.getNbt().getList(WrittenBookItem.PAGES_KEY, NbtElement.STRING_TYPE);
            if (lines.isEmpty()) {
                this.lines = NO_DATA;
            } else {
                var list = new ArrayList<String>(lines.size());
                for (var x : lines) {
                    var text = x.asString();
                    if (!text.isEmpty()) {
                        list.add(text);
                    }
                }
                this.lines = list.toArray(new String[0]);
            }
        } else if (this.stack.isOf(Items.WRITTEN_BOOK) && this.stack.hasNbt()) {
            var lines = this.stack.getNbt().getList(WrittenBookItem.PAGES_KEY, NbtElement.STRING_TYPE);
            if (lines.isEmpty()) {
                this.lines = NO_DATA;
            } else {
                var list = new ArrayList<String>(lines.size());
                for (var x : lines) {
                    var text = Text.Serialization.fromLenientJson(x.asString());
                    if (text != null) {
                        list.add(text.getString());
                    }
                }
                this.lines = list.toArray(new String[0]);
            }
        } else if (this.stack.getItem() instanceof MusicDiscItem disc) {
            this.lines = new String[] { disc.getDescription().getString() };
        } else if (!this.stack.isEmpty()) {
            this.lines = new String[] { this.stack.getName().getString() };
        } else {
            this.lines = NO_DATA;
        }

        this.page = this.page % this.lines.length;
        this.lastData = new StringData(this.lines[this.page]);
        if (this.world != null) {
            if (FactoryBlocks.ITEM_READER.sendData(this.world, this.pos, this.lastData) > 0) {
                if (FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayerEntity player) {
                    TriggerCriterion.trigger(player, FactoryTriggers.ITEM_READER);
                }
            }
        }
    }
    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.stack.isEmpty();
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    public DataContainer nextPage() {
        this.page = (this.page + 1) % lines.length;
        this.lastData = new StringData(this.lines[this.page]);
        return this.lastData;
    }

    @Override
    protected void createGui(ServerPlayerEntity playerEntity) {
        new Gui(playerEntity);
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = (ItemReaderBlock.Model) BlockBoundAttachment.get(chunk, this.pos).holder();
        this.model.setItem(this.stack);
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.HOPPER, player, false);
            this.setTitle(GuiTextures.CENTER_SLOT_GENERIC.apply(ItemReaderBlockEntity.this.getCachedState().getBlock().getName()));
            this.setSlotRedirect(2, new Slot(ItemReaderBlockEntity.this, 0, 0, 0));
            this.open();
        }

        @Override
        public void onClose() {
            super.onClose();
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(ItemReaderBlockEntity.this.pos)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }
}
