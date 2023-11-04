package eu.pb4.polyfactory.block.mechanical.machines;

import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.advancement.TriggerCriterion;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.base.LockableBlockEntity;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.other.OwnedBlockEntity;
import eu.pb4.polyfactory.entity.FactoryPlayer;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.PredicateLimitedSlot;
import eu.pb4.polyfactory.ui.TagLimitedSlot;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.ServerPlayNetExt;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polyfactory.util.inventory.SingleStackInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.OperatorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PlacerBlockEntity extends LockableBlockEntity implements SingleStackInventory, SidedInventory, OwnedBlockEntity {
    private ItemStack stack = ItemStack.EMPTY;
    protected GameProfile owner = null;
    protected double process = 0;
    private float stress = 0;
    private PlacerBlock.Model model;

    public PlacerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PLACER, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.put("stack", this.stack.writeNbt(new NbtCompound()));
        nbt.putDouble("progress", this.process);
        if (this.owner != null) {
            nbt.put("owner", NbtHelper.writeGameProfile(new NbtCompound(), this.owner));
        }
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.stack = ItemStack.fromNbt(nbt.getCompound("tool"));
        this.process = nbt.getDouble("progress");
        if (nbt.contains("owner")) {
            this.owner = NbtHelper.toGameProfile(nbt.getCompound("owner"));
        }
        super.readNbt(nbt);
    }

    @Override
    public ItemStack getStack() {
        return this.stack;
    }

    @Override
    public void setStack(ItemStack stack) {
        this.stack = stack;
        if (this.model != null) {
            this.model.setItem(stack.copyWithCount(1));
        }
    }
    @Override
    public int getMaxCountPerStack() {
        return 64;
    }

    @Override
    protected void createGui(ServerPlayerEntity playerEntity) {
        new Gui(playerEntity);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        var self = (PlacerBlockEntity) t;

        if (self.model == null) {
            self.model = (PlacerBlock.Model) BlockBoundAttachment.get(world, pos).holder();
            self.model.setItem(self.stack);
        }

        var blockPos = pos.offset(state.get(PlacerBlock.FACING));

        if (self.stack.isEmpty() || !(self.stack.getItem() instanceof BlockItem blockItem)) {
            self.stress = 0;
            self.model.setItem(ItemStack.EMPTY);
            return;
        }
        if (!CommonProtection.canPlaceBlock(world, blockPos, self.owner == null ? FactoryUtil.GENERIC_PROFILE : self.owner,null)) {
            self.stress = 0;
            return;
        }

        var dir = state.get(PlacerBlock.FACING);
        var context = new AutomaticItemPlacementContext(world, blockPos, state.get(PlacerBlock.FACING), self.stack, dir.getOpposite()) {
            @Override
            public boolean canReplaceExisting() {
                return true;
            }
        };

        var speed = Math.abs(RotationUser.getRotation((ServerWorld) world, pos).speed()) * MathHelper.RADIANS_PER_DEGREE * 2.5f;

        if (!context.canPlace()) {
            self.model.setItem(ItemStack.EMPTY);
            self.stress = 0;
            return;
        } else {
            self.model.setItem(self.stack.copyWithCount(1));
        }
        self.stress = (float) Math.min(Math.max(8, Math.log(blockItem.getBlock().getHardness()) / Math.log(1.1)), 18);

        if (speed == 0) {
            return;
        }

        self.process += speed / 30;

        self.model.rotate((float) self.process);

        if (self.process >= 1) {
            self.process = 0;
            self.stress = 0;
            blockItem.place(context);

            if (world.getBlockEntity(blockPos) instanceof OwnedBlockEntity ownedBlockEntity) {
                ownedBlockEntity.setOwner(self.owner);
            }

            if (self.owner != null && world.getPlayerByUuid(self.owner.getId()) instanceof ServerPlayerEntity serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.PLACER_PLACES);
            }
            self.markDirty();
        }
    }

    public float getStress() {
        return this.stress;
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

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.HOPPER, player, false);
            this.setTitle(GuiTextures.CENTER_SLOT_GENERIC.apply(PlacerBlockEntity.this.getCachedState().getBlock().getName()));
            this.setSlotRedirect(2, new PredicateLimitedSlot(PlacerBlockEntity.this, 0, s -> s.getItem() instanceof BlockItem));
            this.open();
        }

        @Override
        public void onClose() {
            super.onClose();
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(PlacerBlockEntity.this.pos)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }
}
