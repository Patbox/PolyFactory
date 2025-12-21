package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.block.data.util.ChanneledDataCache;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.ItemStackData;
import eu.pb4.polyfactory.data.StringData;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.SingleStackContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class ItemReaderBlockEntity extends ChanneledDataBlockEntity implements SingleStackContainer, ChanneledDataCache, BlockEntityExtraListener {
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
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.stack = view.read("stack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.page = view.getIntOr("page", 0);
        forceUpdate();
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.store("stack", ItemStack.OPTIONAL_CODEC, this.stack);
        view.putInt("page", this.page);
    }

    @Override
    public void setStack(ItemStack stack) {
        this.stack = stack;
        this.page = 0;
        this.forceUpdate();
        this.setChanged();
    }

    private void forceUpdate() {
        if (this.model != null) {
            this.model.setItem(this.stack);
        }

        if (this.stack.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
            var lines = this.stack.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY).pages();
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
        } else if (this.stack.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
            var lines = this.stack.getOrDefault(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY).pages();
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
        } else if (this.stack.has(DataComponents.JUKEBOX_PLAYABLE)) {
            var song = JukeboxSong.fromStack(this.level.registryAccess(), this.stack);
            this.lines = song.map(jukeboxSongRegistryEntry -> new String[]{jukeboxSongRegistryEntry.value().description().getString()}).orElseGet(() -> new String[]{this.stack.getHoverName().getString()});
        } else if (!this.stack.isEmpty()) {
            this.lines = new String[]{this.stack.getHoverName().getString()};
        } else {
            this.lines = NO_DATA;
        }

        this.page = this.page % this.lines.length;
        this.lastData = new ItemStackData(this.stack.copy(), this.lines[this.page]);
        if (this.level != null) {
            if (DataProvider.sendData(this.level, this.worldPosition, this.lastData) > 0) {
                if (FactoryUtil.getClosestPlayer(level, worldPosition, 32) instanceof ServerPlayer player) {
                    TriggerCriterion.trigger(player, FactoryTriggers.ITEM_READER);
                }
            }
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.stack.isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public DataContainer nextPage() {
        this.page = (this.page + 1) % lines.length;
        this.lastData = new StringData(this.lines[this.page]);
        return this.lastData;
    }

    @Override
    protected void createGui(ServerPlayer playerEntity) {
        new Gui(playerEntity);
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        this.model = (ItemReaderBlock.Model) BlockBoundAttachment.get(chunk, this.worldPosition).holder();
        this.model.setItem(this.stack);
        super.onListenerUpdate(chunk);
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayer player) {
            super(MenuType.HOPPER, player, false);
            this.setTitle(GuiTextures.CENTER_SLOT_GENERIC.apply(ItemReaderBlockEntity.this.getBlockState().getBlock().getName()));
            this.setSlotRedirect(2, new Slot(ItemReaderBlockEntity.this, 0, 0, 0));
            this.open();
        }

        @Override
        public void onClose() {
            super.onClose();
        }

        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(ItemReaderBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }
}
