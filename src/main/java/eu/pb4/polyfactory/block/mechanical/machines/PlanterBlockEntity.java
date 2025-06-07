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
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.TagLimitedSlot;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.MinimalSidedInventory;
import eu.pb4.polyfactory.util.inventory.SingleStackInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public class PlanterBlockEntity extends LockableBlockEntity implements MinimalSidedInventory, BlockEntityExtraListener, OwnedBlockEntity {
    private static final int[] SLOTS = IntStream.range(0, 9).toArray();
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(9, ItemStack.EMPTY);
    protected GameProfile owner = null;
    protected double process = 0;
    private float stress = 0;
    private int radius = 2;
    private PlanterBlock.Model model;

    public PlanterBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PLANTER, pos, state);
    }

    @Override
    protected void writeData(WriteView view) {
        Inventories.writeData(view, this.items);
        view.putDouble("progress", this.process);
        if (this.owner != null) {
            view.put("owner", NbtCompound.CODEC, LegacyNbtHelper.writeGameProfile(new NbtCompound(), this.owner));
        }
        view.putInt("radius", this.radius);
        super.writeData(view);
    }

    @Override
    public void readData(ReadView view) {
        if (!view.getReadView("stack").getString("id", "").isEmpty() && view.getListReadView("Items").isEmpty()) {
            this.setStack(0, view.read("stack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        } else {
            Inventories.readData(view, this.items);
        }

        this.process = view.getDouble("progress", 0);
        view.read("owner", NbtCompound.CODEC).ifPresent(x -> this.owner = LegacyNbtHelper.toGameProfile(x));

        this.radius = view.getInt("radius", 1);

        super.readData(view);
    }

    @Override
    public GameProfile getOwner() {
        return this.owner;
    }

    @Override
    public void setOwner(GameProfile profile) {
        this.owner = profile;
        this.markDirty();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return stack.isIn(FactoryItemTags.ALLOWED_IN_PLANTER);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    protected void createGui(ServerPlayerEntity playerEntity) {
        new Gui(playerEntity);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
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

        var placableState = blockItem.getBlock().getDefaultState();

        var speed = Math.abs(RotationUser.getRotation((ServerWorld) world, pos).speed()) * MathHelper.RADIANS_PER_DEGREE * 2.5f;

        BlockPos place = null;
        for (var y = 0; y < 2; y++) {
            for (var mut : BlockPos.iterateInSquare(pos.down(y), self.radius, Direction.NORTH, Direction.WEST)) {
                var targetState = world.getBlockState(mut);
                if ((targetState.isAir() || (targetState.isReplaceable() && targetState.getFluidState().isEmpty()))
                        && placableState.canPlaceAt(world, mut)
                        && CommonProtection.canPlaceBlock(world, mut, self.owner == null ? FactoryUtil.GENERIC_PROFILE : self.owner, null)) {
                    place = mut.toImmutable();
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
            world.setBlockState(place, placableState);
            stack.decrement(1);
            world.playSound(null, pos, placableState.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 0.5f, 1.0f);
            world.emitGameEvent(GameEvent.BLOCK_PLACE, place, GameEvent.Emitter.of(placableState));
            if (self.owner != null && world.getPlayerByUuid(self.owner.getId()) instanceof ServerPlayerEntity serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.PLANTER_PLANTS);
            }
            self.markDirty();
        }
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
    }

    public float getStress() {
        return this.stress;
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = (PlanterBlock.Model) BlockBoundAttachment.get(chunk, this.pos).holder();
    }

    public int radius() {
        return this.radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
        this.markDirty();
    }

    @Override
    public DefaultedList<ItemStack> getStacks() {
        return this.items;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_3X3, player, false);
            this.setTitle(PlanterBlockEntity.this.getCachedState().getBlock().getName());
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
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(PlanterBlockEntity.this.pos)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }
}
