package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.GuiUtils;
import eu.pb4.polyfactory.util.ContainerSavingHelper;
import eu.pb4.polyfactory.util.inventory.MinimalSidedContainer;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
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

public class DeepStorageContainerBlockEntity extends LockableBlockEntity implements MinimalSidedContainer {
    private static final int PAGE = 6 * 7;
    private static final int PAGE_COUNT = 8;
    private static final int SIZE = PAGE * PAGE_COUNT;
    private static final int[] SLOTS = IntStream.range(0, SIZE).toArray();

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            DeepStorageContainerBlockEntity.this.playSound(state, SoundEvents.COPPER_CHEST_OPEN);
            DeepStorageContainerBlockEntity.this.updateBlockState(state, true);
        }

        protected void onClose(Level level, BlockPos pos, BlockState state) {
            DeepStorageContainerBlockEntity.this.playSound(state, SoundEvents.COPPER_CHEST_CLOSE);
            DeepStorageContainerBlockEntity.this.updateBlockState(state, false);
        }

        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int count, int openCount) {
        }

        public boolean isOwnContainer(Player player) {
            if (player instanceof ServerPlayer serverPlayer && GuiHelpers.getCurrentGui(serverPlayer) instanceof Gui gui) {
                return gui.getContainer() == DeepStorageContainerBlockEntity.this;
            } else {
                return false;
            }
        }
    };
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final NonNullList<ItemStack> iconOverrides = NonNullList.withSize(8, ItemStack.EMPTY);

    public DeepStorageContainerBlockEntity(BlockPos pos, BlockState state) {
        this(FactoryBlockEntities.DEEP_STORAGE_CONTAINER, pos, state);
    }

    protected DeepStorageContainerBlockEntity(BlockEntityType<? extends DeepStorageContainerBlockEntity> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        ContainerSavingHelper.saveAllItems(view, this.stacks);
        ContainerSavingHelper.saveAllItems("icon_override", view, this.iconOverrides);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        ContainerSavingHelper.loadAllItems(view, this.stacks);
        ContainerSavingHelper.loadAllItems("icon_override", view, this.iconOverrides);
    }

    @Override
    protected void createGui(ServerPlayer playerEntity) {
        new Gui(playerEntity);
    }

    public int getComparatorOutput() {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(this);
    }

    @Override
    public NonNullList<ItemStack> getStacks() {
        return this.stacks;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack itemStack, @Nullable Direction direction) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    void updateBlockState(BlockState state, boolean open) {
        this.level.setBlock(this.getBlockPos(), state.setValue(DeepStorageContainerBlock.OPEN, open), 3);
    }

    void playSound(BlockState state, SoundEvent sound) {
        Vec3i vec3i = state.getValue(DeepStorageContainerBlock.FACING).getUnitVec3i();
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

    private class Gui extends SimpleGui {
        private int page = -1;

        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x6, player, false);
            this.setPage(0);

            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 4; y++) {
                    int i = x + y * 2;
                    this.setSlot(x + y * 9 + 7 + 9, GuiUtils.createDynamicButton(() -> getButtonIcon(i), clickType -> {
                        if (clickType.isMiddle || clickType.isRight) {
                            var carried = screenHandler.getCarried();
                            var oldOverride = iconOverrides.get(i);
                            if (!ItemStack.isSameItemSameComponents(carried, oldOverride)) {
                                carried = carried.copyWithCount(1);
                                iconOverrides.set(i, carried);
                                setChanged();
                                GuiUtils.playClickSound(this.player);
                            }
                            return;
                        }

                        if (this.setPage(i)) {
                            GuiUtils.playClickSound(this.player);
                        }
                    }));
                }
            }
            this.setSlot(7, GuiUtils.createDynamicButton(this::getSelectedIcon));

            this.open();
            startOpen(player);
        }

        private ItemStack getButtonIcon(int page) {
            if (iconOverrides.get(page).isEmpty()) {
                return GuiTextures.NUMBERS_SHADOW_8[page + 1].apply(page == this.page ? 0xFFFF88 : 0xFFFFFF).hideTooltip().asStack();
            } else {
                return GuiElementBuilder.from(iconOverrides.get(page)).hideTooltip().asStack();
            }
        }

        private ItemStack getSelectedIcon() {
            var stack = GuiTextures.DEEP_STORAGE_UNIT_SELECTED.copy();
            var selected = new BooleanArrayList(8);
            for (int i = 0; i < PAGE_COUNT; i++) {
                selected.add(i == this.page);
            }
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), selected, List.of(), List.of()));
            return stack;
        }

        private boolean setPage(int page) {
            if (this.page == page) {
                return false;
            }
            this.page = page;

            for (int x = 0; x < 7; x++) {
                for (int y = 0; y < 6; y++) {
                    this.setSlotRedirect(x + y * 9, new Slot(DeepStorageContainerBlockEntity.this, x + y * 7 + page * PAGE, x, y));
                }
            }

            this.setTitle(GuiTextures.DEEP_STORAGE_CONTAINER.apply(Component.translatable("text.polyfactory.thing_x_out_of_y",
                    DeepStorageContainerBlockEntity.this.getBlockState().getBlock().getName(),
                    this.page + 1,
                    PAGE_COUNT
            )));

            return true;
        }

        @Override
        public void onScreenHandlerClosed() {
            super.onScreenHandlerClosed();
            stopOpen(player);
        }

        @Override
        public void onTick() {
            if (DeepStorageContainerBlockEntity.this.isRemoved() || player.position().distanceToSqr(Vec3.atCenterOf(DeepStorageContainerBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }

        public DeepStorageContainerBlockEntity getContainer() {
            return DeepStorageContainerBlockEntity.this;
        }
    }
}
