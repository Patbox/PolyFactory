package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.BarrierBasedWaterloggable;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.mixin.AbstractContainerMenuAccessor;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.sgui.api.GuiHelpers;
import eu.pb4.sgui.api.ScreenProperty;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoldMakingTableBlock extends Block implements FactoryBlock, BarrierBasedWaterloggable {
    public MoldMakingTableBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
        super.createBlockStateDefinition(builder);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return waterLog(ctx, this.defaultBlockState());
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        tickWater(state, world, tickView, pos);
        return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.CRAFTING_TABLE.defaultBlockState();
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            new Gui(serverPlayer, pos);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        Containers.updateNeighboursAfterDestroy(state, world, pos);
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    public static class Model extends BlockModel {
        private final ItemDisplayElement base;

        public Model(BlockState state) {
            this.base = ItemDisplayElementUtil.createSolid(state.getBlock().asItem());
            this.base.setScale(new Vector3f(2));
            this.addElement(this.base);
        }
    }

    public class Gui extends SimpleGui {
        private final List<Holder<Item>> items;
        private final ResultContainer result = new ResultContainer();
        private final Slot inputSlot;
        private final Slot resultSlot;
        private Item previousItem = Items.AIR;
        private int selectedItem = -1;
        private final Container input = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                setupResultSlot();
            }
        };

        public Gui(ServerPlayer player, BlockPos pos) {
            super(MenuType.STONECUTTER, player, false);
            this.setTitle(getName());
            this.items = FactoryUtil.collect(player.registryAccess().lookupOrThrow(Registries.ITEM).getTagOrEmpty(FactoryItemTags.SHAPEABLE_CLAY_MOLDS));
            this.setSlotRedirect(0, this.inputSlot = new Slot(input, 0, 0, 0));
            this.setSlotRedirect(1, this.resultSlot = new Slot(result, 0, 0, 0) {
                private long lastSoundTime;

                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return hasCorrectInputItem(inputSlot.getItem());
                }

                public void onTake(Player player, ItemStack stack) {
                    stack.onCraftedBy(player, stack.getCount());
                    var level = player.level();
                    ItemStack itemStack = inputSlot.remove(inputSlot.getItem().is(Items.CLAY_BALL) ? 4 : 1);
                    if (!itemStack.isEmpty()) {
                        setupResultSlot();
                    }
                    long l = level.getGameTime();
                    if (lastSoundTime != l) {
                        level.playSound(null, pos, SoundType.GRAVEL.getBreakSound(), SoundSource.BLOCKS, 0.8F, 1.3F);
                        lastSoundTime = l;
                    }
                    super.onTake(player, stack);
                }
            });

            var recipeManager = this.player.level().recipeAccess();
            this.player.connection.send(new ClientboundUpdateRecipesPacket(recipeManager.getSynchronizedItemProperties(), new SelectableRecipe.SingleInputSet<>(List.of())));
            this.open();
        }

        @Override
        public void onClose() {
            var recipeManager = this.player.level().recipeAccess();
            this.player.connection.send(new ClientboundUpdateRecipesPacket(recipeManager.getSynchronizedItemProperties(), recipeManager.getSynchronizedStonecutterRecipes()));
        }

        @Override
        public void onPlayerClose(boolean success) {
            AbstractContainerMenuAccessor.callDropOrPlaceInInventory(this.player, this.screenHandler.getCarried());
            this.screenHandler.setCarried(ItemStack.EMPTY);
            AbstractContainerMenuAccessor.callDropOrPlaceInInventory(this.player, this.input.getItem(0));
            this.input.clearContent();
            this.result.clearContent();
            super.onPlayerClose(success);
        }

        @Override
        public boolean onButtonClick(int id) {
            this.selectedItem = id;
            this.setupResultSlot();
            return true;
        }

        @Override
        public ItemStack quickMove(int index) {
            var resultStack = ItemStack.EMPTY;
            var slot = this.screenHandler.slots.get(index);
            if (slot != null && slot.hasItem()) {
                ItemStack slotStack = slot.getItem();
                resultStack = slotStack.copy();
                if (index == 1) {
                    slotStack.getItem().onCraftedBy(slotStack, player);
                    if (!this.insertItem(slotStack, 2, 38, true)) {
                        return ItemStack.EMPTY;
                    }

                    slot.onQuickCraft(slotStack, resultStack);
                } else if (index == 0) {
                    if (!this.insertItem(slotStack, 2, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (hasCorrectInputItem(slotStack)) {
                    if (!this.insertItem(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 2 && index < 29) {
                    if (!this.insertItem(slotStack, 29, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 29 && index < 38 && !this.insertItem(slotStack, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }

                if (slotStack.isEmpty()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                }

                slot.setChanged();
                if (slotStack.getCount() == resultStack.getCount()) {
                    return ItemStack.EMPTY;
                }

                slot.onTake(player, slotStack);
                if (index == 1) {
                    player.drop(slotStack, false);
                }
            }

            return resultStack;
        }

        private void setupResultSlot() {
            var isCorrectItem = hasCorrectInputItem(this.inputSlot.getItem());
            if (isCorrectItem) {
                var ingredient = Ingredient.of(
                        this.inputSlot.getItem().getItem() instanceof PolymerItem polymerItem
                                ? PolymerItemUtils.getItemSafely(polymerItem, this.inputSlot.getItem(), PacketContext.create(this.player)).item()
                                : this.inputSlot.getItem().getItem()
                );

                if (this.previousItem != this.inputSlot.getItem().getItem()) {
                    this.previousItem = this.inputSlot.getItem().getItem();
                    var fakeRecipes = new ArrayList<SelectableRecipe.SingleInputEntry<StonecutterRecipe>>();
                    for (var item : this.items) {
                        fakeRecipes.add(new SelectableRecipe.SingleInputEntry<>(ingredient,
                                new SelectableRecipe<>(new SlotDisplay.ItemSlotDisplay(item), Optional.empty())));
                    }
                    var recipeManager = this.player.level().recipeAccess();
                    this.player.connection.send(new ClientboundUpdateRecipesPacket(recipeManager.getSynchronizedItemProperties(), new SelectableRecipe.SingleInputSet<>(fakeRecipes)));

                    GuiHelpers.sendSlotUpdate(player, this.syncId, 0, ItemStack.EMPTY);
                    GuiHelpers.sendSlotUpdate(player, this.syncId, 0, this.inputSlot.getItem());
                    this.sendProperty(ScreenProperty.SELECTED, this.selectedItem);
                }
            } else if (this.previousItem != Items.AIR) {
                var recipeManager = this.player.level().recipeAccess();
                this.player.connection.send(new ClientboundUpdateRecipesPacket(recipeManager.getSynchronizedItemProperties(), new SelectableRecipe.SingleInputSet<>(List.of())));
                this.previousItem = Items.AIR;

                GuiHelpers.sendSlotUpdate(player, this.syncId, 0, ItemStack.EMPTY);
                GuiHelpers.sendSlotUpdate(player, this.syncId, 0, this.inputSlot.getItem());
                this.sendProperty(ScreenProperty.SELECTED, this.selectedItem);
            }

            if (!isCorrectItem || this.selectedItem < 0 || this.selectedItem >= this.items.size()) {
                this.result.setItem(0, ItemStack.EMPTY);
            } else {
                this.result.setItem(0, this.items.get(this.selectedItem).value().getDefaultInstance());
            }
        }

        private boolean hasCorrectInputItem(ItemStack item) {
            return item.is(Items.CLAY) || item.is(FactoryItemTags.SHAPEABLE_CLAY_MOLDS) || (item.is(Items.CLAY_BALL) && item.getCount() >= 4);
        }
    }
}
