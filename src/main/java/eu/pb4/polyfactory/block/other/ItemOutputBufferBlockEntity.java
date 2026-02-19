package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.util.ContainerSavingHelper;
import eu.pb4.polyfactory.util.inventory.RedirectingWorldlyfiedContainer;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.IntStream;

public class ItemOutputBufferBlockEntity extends LockableBlockEntity implements RedirectingWorldlyfiedContainer {
    private static final int SIZE = 9 * 2;

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            ItemOutputBufferBlockEntity.this.playSound(state, SoundEvents.BARREL_OPEN);
            ItemOutputBufferBlockEntity.this.updateBlockState(state, true);
        }

        protected void onClose(Level level, BlockPos pos, BlockState state) {
            ItemOutputBufferBlockEntity.this.playSound(state, SoundEvents.BARREL_CLOSE);
            ItemOutputBufferBlockEntity.this.updateBlockState(state, false);
        }

        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int count, int openCount) {
        }

        public boolean isOwnContainer(Player player) {
            if (player instanceof ServerPlayer serverPlayer && GuiHelpers.getCurrentGui(serverPlayer) instanceof Gui gui) {
                return gui.getContainer() == ItemOutputBufferBlockEntity.this;
            } else {
                return false;
            }
        }
    };
    private final SimpleContainer container = new SimpleContainer(SIZE);
    private int[] slots = new int[0];
    private boolean isActive = true;


    public ItemOutputBufferBlockEntity(BlockPos pos, BlockState state) {
        this(FactoryBlockEntities.ITEM_BUFFER, pos, state);
    }

    protected ItemOutputBufferBlockEntity(BlockEntityType<? extends ItemOutputBufferBlockEntity> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        this.isActive = false;
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerSavingHelper.saveAllItems(view, this.container.getItems());
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        ContainerSavingHelper.loadAllItems(view, this.container.getItems());
    }

    @Override
    protected void createGui(ServerPlayer playerEntity) {
        this.createGui(playerEntity, false);
    }

    protected void createGui(ServerPlayer playerEntity, boolean canInsert) {
        new Gui(playerEntity, canInsert);
    }

    public int getComparatorOutput() {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(this);
    }


    void updateBlockState(BlockState state, boolean open) {
        this.level.setBlock(this.getBlockPos(), state.setValue(DeepStorageContainerBlock.OPEN, open), 3);
    }

    void playSound(BlockState state, SoundEvent sound) {
        Vec3i vec3i = state.getValue(ItemOutputBufferBlock.ORIENTATION).front().getUnitVec3i();
        double x = this.worldPosition.getX() + 1 + vec3i.getX();
        double y = this.worldPosition.getY() + 1 + vec3i.getY();
        double z = this.worldPosition.getZ() + 1 + vec3i.getZ();
        this.level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 1.1F);
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator()) {
            this.openersCounter.incrementOpeners(user.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState(), user.getContainerInteractionRange());
        }

    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator()) {
            this.openersCounter.decrementOpeners(user.getLivingEntity(), this.getLevel(), this.getBlockPos(), this.getBlockState());
        }

    }

    @Override
    public @NonNull List<ContainerUser> getEntitiesWithContainerOpen() {
        return this.openersCounter.getEntitiesWithContainerOpen(this.getLevel(), this.getBlockPos());
    }

    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    public Container getOwnContainer() {
        return this.container;
    }

    @Override
    public Container getRedirect() {
        var dir = this.getBlockState().getValue(ItemOutputBufferBlock.ORIENTATION).front();
        if (this.isActive && this.level != null && this.level.getBlockEntity(this.getBlockPos().relative(dir)) instanceof OutputContainerOwner owner && owner.isOutputConnectedTo(dir.getOpposite())) {
            return owner.getOutputContainer();
        }

        return this.container;
    }


    @Override
    public int[] getSlotsForFace(Direction side) {
        var red = this.getRedirect();
        if (this.slots == null || this.slots.length != red.getContainerSize()) {
            this.slots = IntStream.range(0, red.getContainerSize()).toArray();
        }

        return this.slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return this.getBlockState().getValue(ItemOutputBufferBlock.ORIENTATION).front() == direction;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    public boolean isConnected() {
        return getRedirect() != this.container;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayer player, boolean mayPlace) {
            super(MenuType.GENERIC_9x2, player, false);
            this.setTitle(ItemOutputBufferBlockEntity.this.getContainerName());

            for (int i = 0; i < SIZE; i++) {
                this.setSlotRedirect(i, new Slot(ItemOutputBufferBlockEntity.this.container, i, i, 0) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return mayPlace;
                    }
                });
            }

            this.open();
            startOpen(player);
        }

        @Override
        public void onScreenHandlerClosed() {
            super.onScreenHandlerClosed();
            stopOpen(player);
        }

        @Override
        public void onTick() {
            if (ItemOutputBufferBlockEntity.this.isRemoved() || player.position().distanceToSqr(Vec3.atCenterOf(ItemOutputBufferBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }

        public ItemOutputBufferBlockEntity getContainer() {
            return ItemOutputBufferBlockEntity.this;
        }
    }
}
