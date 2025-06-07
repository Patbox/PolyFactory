package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.ItemStackData;
import eu.pb4.polyfactory.data.StringData;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.SingleStackInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WritableBookContentComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class ItemReaderBlockEntity extends ChanneledDataBlockEntity implements SingleStackInventory, ChanneledDataCache, BlockEntityExtraListener {
    private static final String[] NO_DATA = new String[]{""};
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
    public void readData(ReadView view) {
        super.readData(view);
        this.stack = view.read("stack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.page = view.getInt("page", 0);
        forceUpdate();
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.put("stack", ItemStack.OPTIONAL_CODEC, this.stack);
        view.putInt("page", this.page);
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

        if (this.stack.contains(DataComponentTypes.WRITABLE_BOOK_CONTENT)) {
            var lines = this.stack.getOrDefault(DataComponentTypes.WRITABLE_BOOK_CONTENT, WritableBookContentComponent.DEFAULT).pages();
            if (lines.isEmpty()) {
                this.lines = NO_DATA;
            } else {
                var list = new ArrayList<String>(lines.size());
                for (var x : lines) {
                    var text = x.raw();
                    if (!text.isEmpty()) {
                        list.add(text);
                    }
                }
                this.lines = list.toArray(new String[0]);
            }
        } else if (this.stack.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT)) {
            var lines = this.stack.getOrDefault(DataComponentTypes.WRITTEN_BOOK_CONTENT, WrittenBookContentComponent.DEFAULT).pages();
            if (lines.isEmpty()) {
                this.lines = NO_DATA;
            } else {
                var list = new ArrayList<String>(lines.size());
                for (var x : lines) {
                    var text = x.raw();
                    if (text != null) {
                        list.add(text.getString());
                    }
                }
                this.lines = list.toArray(new String[0]);
            }
        } else if (this.stack.contains(DataComponentTypes.JUKEBOX_PLAYABLE)) {
            var song = JukeboxSong.getSongEntryFromStack(this.world.getRegistryManager(), this.stack);
            this.lines = song.map(jukeboxSongRegistryEntry -> new String[]{jukeboxSongRegistryEntry.value().description().getString()}).orElseGet(() -> new String[]{this.stack.getName().getString()});
        } else if (!this.stack.isEmpty()) {
            this.lines = new String[]{this.stack.getName().getString()};
        } else {
            this.lines = NO_DATA;
        }

        this.page = this.page % this.lines.length;
        this.lastData = new ItemStackData(this.stack.copy(), this.lines[this.page]);
        if (this.world != null) {
            if (DataProvider.sendData(this.world, this.pos, this.lastData) > 0) {
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
        super.onListenerUpdate(chunk);
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
