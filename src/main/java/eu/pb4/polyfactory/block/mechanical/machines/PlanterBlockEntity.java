package eu.pb4.polyfactory.block.mechanical.machines;

import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.OwnedBlockEntity;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.factorytools.api.util.LegacyNbtHelper;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.ui.TagLimitedSlot;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.MinimalSidedContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class PlanterBlockEntity extends LockableBlockEntity implements MinimalSidedContainer, BlockEntityExtraListener, OwnedBlockEntity {
    private static final int[] SLOTS = IntStream.range(0, 9).toArray();
    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
    protected GameProfile owner = null;
    protected double process = 0;
    private float stress = 0;
    private int radius = 2;
    private PlanterBlock.Model model;

    public PlanterBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PLANTER, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        ContainerHelper.saveAllItems(view, this.items);
        view.putDouble("progress", this.process);
        if (this.owner != null) {
            view.store("owner", CompoundTag.CODEC, LegacyNbtHelper.writeGameProfile(new CompoundTag(), this.owner));
        }
        view.putInt("radius", this.radius);
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        if (!view.childOrEmpty("stack").getStringOr("id", "").isEmpty() && view.childrenListOrEmpty("Items").isEmpty()) {
            this.setItem(0, view.read("stack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        } else {
            ContainerHelper.loadAllItems(view, this.items);
        }

        this.process = view.getDoubleOr("progress", 0);
        view.read("owner", CompoundTag.CODEC).ifPresent(x -> this.owner = LegacyNbtHelper.toGameProfile(x));

        this.radius = view.getIntOr("radius", 1);

        super.loadAdditional(view);
    }

    @Override
    public GameProfile getOwner() {
        return this.owner;
    }

    @Override
    public void setOwner(GameProfile profile) {
        this.owner = profile;
        this.setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return stack.is(FactoryItemTags.ALLOWED_IN_PLANTER);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    protected void createGui(ServerPlayer playerEntity) {
        new Gui(playerEntity);
    }

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
        var self = (PlanterBlockEntity) t;

        if (self.isEmpty()) {
            self.stress = 0;
            self.process = 0;
            return;
        }
        ItemStack stack = ItemStack.EMPTY;
        for (var x : self.items) {
            if (!x.isEmpty()) {
                stack = x;
                break;
            }
        }

        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            self.stress = 0;
            self.process = 0;
            return;
        }

        var placableState = blockItem.getBlock().defaultBlockState();

        var speed = Math.abs(RotationUser.getRotation((ServerLevel) world, pos).speed()) * Mth.DEG_TO_RAD * 2.5f;

        BlockPos place = null;
        for (var y = 0; y < 2; y++) {
            for (var mut : BlockPos.spiralAround(pos.below(y), self.radius, Direction.NORTH, Direction.WEST)) {
                var targetState = world.getBlockState(mut);
                if ((targetState.isAir() || (targetState.canBeReplaced() && targetState.getFluidState().isEmpty()))
                        && placableState.canSurvive(world, mut)
                        && CommonProtection.canPlaceBlock(world, mut, self.owner == null ? FactoryUtil.GENERIC_PROFILE : self.owner, null)) {
                    place = mut.immutable();
                    break;
                }
            }
            if (place != null) {
                break;
            }
        }

        if (place == null) {
            self.stress = 0;
            self.process = 0;
            return;
        }

        self.stress = 6;

        if (speed == 0) {
            return;
        }
        self.model.setDirection(pos, place);

        if (self.process == 0) {
            self.process += speed / 40;
            return;
        }
        self.process += speed / 40;

        if (self.process >= 1) {
            self.process = 0;
            self.stress = 0;
            world.setBlockAndUpdate(place, placableState);
            stack.shrink(1);
            world.playSound(null, pos, placableState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.5f, 1.0f);
            world.gameEvent(GameEvent.BLOCK_PLACE, place, GameEvent.Context.of(placableState));
            if (self.owner != null && world.getPlayerByUUID(self.owner.id()) instanceof ServerPlayer serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.PLANTER_PLANTS);
            }
            self.setChanged();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    public float getStress() {
        return this.stress;
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        this.model = (PlanterBlock.Model) BlockBoundAttachment.get(chunk, this.worldPosition).holder();
    }

    public int radius() {
        return this.radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
        this.setChanged();
    }

    @Override
    public NonNullList<ItemStack> getStacks() {
        return this.items;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_3x3, player, false);
            this.setTitle(PlanterBlockEntity.this.getBlockState().getBlock().getName());
            for (int i = 0; i < 9; i++) {
                this.setSlotRedirect(i, new TagLimitedSlot(PlanterBlockEntity.this, i, FactoryItemTags.ALLOWED_IN_PLANTER));
            }
            this.open();
        }

        @Override
        public void onClose() {
            super.onClose();
        }

        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(PlanterBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }
}
