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
import eu.pb4.polyfactory.util.inventory.SingleStackInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public class PlanterBlockEntity extends LockableBlockEntity implements SingleStackInventory, SidedInventory, BlockEntityExtraListener, OwnedBlockEntity {
    private ItemStack stack = ItemStack.EMPTY;
    protected GameProfile owner = null;
    protected double process = 0;
    private float stress = 0;
    private int radius = 2;
    private PlanterBlock.Model model;

    public PlanterBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PLANTER, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.put("stack", this.stack.toNbtAllowEmpty(lookup));
        nbt.putDouble("progress", this.process);
        if (this.owner != null) {
            nbt.put("owner", LegacyNbtHelper.writeGameProfile(new NbtCompound(), this.owner));
        }
        nbt.putInt("radius", this.radius);
        super.writeNbt(nbt, lookup);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        this.stack = ItemStack.fromNbtOrEmpty(lookup, nbt.getCompound("stack"));
        this.process = nbt.getDouble("progress");
        if (nbt.contains("owner")) {
            this.owner = LegacyNbtHelper.toGameProfile(nbt.getCompound("owner"));
        }
        if (nbt.contains("radius")) {
            this.radius = nbt.getInt("radius");
        } else {
            this.radius = 1;
        }

        super.readNbt(nbt, lookup);
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
    public ItemStack getStack() {
        return this.stack;
    }

    @Override
    public void setStack(ItemStack stack) {
        this.stack = stack;
        this.markDirty();
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return stack.isIn(FactoryItemTags.ALLOWED_IN_PLANTER);
    }

    @Override
    protected void createGui(ServerPlayerEntity playerEntity) {
        new Gui(playerEntity);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        var self = (PlanterBlockEntity) t;

        if (self.stack.isEmpty() || !(self.stack.getItem() instanceof BlockItem blockItem)) {
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
            self.stack.decrement(1);
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

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.HOPPER, player, false);
            this.setTitle(GuiTextures.CENTER_SLOT_GENERIC.apply(PlanterBlockEntity.this.getCachedState().getBlock().getName()));
            this.setSlotRedirect(2, new TagLimitedSlot(PlanterBlockEntity.this, 0, FactoryItemTags.ALLOWED_IN_PLANTER));
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
