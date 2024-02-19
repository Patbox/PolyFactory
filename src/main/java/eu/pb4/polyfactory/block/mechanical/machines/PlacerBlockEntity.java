package eu.pb4.polyfactory.block.mechanical.machines;

import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.factorytools.api.util.FactoryPlayer;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.factorytools.api.block.OwnedBlockEntity;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.PredicateLimitedSlot;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.SingleStackInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PlacerBlockEntity extends LockableBlockEntity implements SingleStackInventory, SidedInventory, OwnedBlockEntity {
    private ItemStack stack = ItemStack.EMPTY;
    protected GameProfile owner = null;
    protected double process = 0;
    private float stress = 0;
    private PlacerBlock.Model model;
    private FactoryPlayer player;

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

        var usable = self.stack.isIn(FactoryItemTags.PLACER_USABLE);

        if (self.stack.isEmpty() || (!(self.stack.getItem() instanceof BlockItem) && !usable)) {
            self.stress = 0;
            self.model.setItem(ItemStack.EMPTY);
            return;
        }
        if (!CommonProtection.canPlaceBlock(world, blockPos, self.owner == null ? FactoryUtil.GENERIC_PROFILE : self.owner,null) ||
        !CommonProtection.canInteractBlock(world, blockPos, self.owner == null ? FactoryUtil.GENERIC_PROFILE : self.owner,null)) {
            self.stress = 0;
            return;
        }

        var dir = state.get(PlacerBlock.FACING);
        var speed = Math.abs(RotationUser.getRotation((ServerWorld) world, pos).speed()) * MathHelper.RADIANS_PER_DEGREE * 2.5f;

        var blockHitResult = new BlockHitResult(Vec3d.ofCenter(blockPos).offset(dir, 0.5), dir.getOpposite(), blockPos, true);

        var context = usable
                ? new ItemUsageContext(world, self.getFakePlayer(), Hand.MAIN_HAND, self.stack, blockHitResult)
                : new AutomaticItemPlacementContext(world, blockPos, state.get(PlacerBlock.FACING), self.stack, dir.getOpposite()) {
            @Override
            public boolean canReplaceExisting() {
                return true;
            }
        };


        if (context instanceof AutomaticItemPlacementContext ctx && !ctx.canPlace()) {
            self.model.setItem(ItemStack.EMPTY);
            self.stress = 0;
            return;
        } else {
            self.model.setItem(self.stack.copyWithCount(1));
        }
        self.stress = self.stack.getItem() instanceof BlockItem blockItem
                ? (float) Math.min(Math.max(8, Math.log(blockItem.getBlock().getHardness()) / Math.log(1.1)), 18)
                : 12
        ;

        if (speed == 0) {
            return;
        }

        self.process += speed / 30;

        if (self.process >= 1) {
            self.process = 0;
            self.stress = 0;
            if (self.stack.getItem() instanceof BlockItem blockItem && context instanceof AutomaticItemPlacementContext ctx) {
                blockItem.place(ctx);
                if (world.getBlockEntity(blockPos) instanceof OwnedBlockEntity ownedBlockEntity) {
                    ownedBlockEntity.setOwner(self.owner);
                }
            } else {
                var p = self.getFakePlayer();
                if (dir == Direction.UP) {
                    p.setYaw(0);
                    p.setPitch(-90);
                } else if (dir == Direction.DOWN) {
                    p.setYaw(0);
                    p.setPitch(90);
                } else  {
                    p.setYaw(dir.asRotation());
                    p.setPitch(0);
                }

                var vec = Vec3d.ofCenter(pos).offset(state.get(PlacerBlock.FACING), 0.51);
                p.setPos(vec.x, vec.y, vec.z);

                var actionResult = world.getBlockState(blockPos).onUse(world, self.getFakePlayer(), Hand.MAIN_HAND, blockHitResult);
                if (!actionResult.isAccepted()) {
                    actionResult = self.stack.useOnBlock(context);
                    if (!actionResult.isAccepted()) {
                        var x = self.stack.use(world, self.getFakePlayer(), Hand.MAIN_HAND);
                        self.setStack(x.getValue());
                        ItemScatterer.spawn(world, pos, p.getInventory());
                    }
                }
            }

            if (self.owner != null && world.getPlayerByUuid(self.owner.getId()) instanceof ServerPlayerEntity serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.PLACER_PLACES);
            }
            self.markDirty();
        }
        self.model.rotate((float) self.process);
    }

    public float getStress() {
        return this.stress;
    }

    @Override
    public GameProfile getOwner() {
        return this.owner;
    }

    public FactoryPlayer getFakePlayer() {
        if (this.player == null) {
            var profile = this.owner == null ? FactoryUtil.GENERIC_PROFILE : this.owner;

            this.player = new FactoryPlayer(StackReference.of(this, 0), (ServerWorld) this.world, this.pos,
                    new GameProfile(profile.getId(), "Placer (" + profile.getName() + ")")) {
                @Override
                public double getEyeY() {
                    return getY();
                }
            };
            var vec = Vec3d.ofCenter(this.pos).offset(this.getCachedState().get(PlacerBlock.FACING), 0.51);

            this.player.setPos(vec.x, vec.y, vec.z);
        }

        return this.player;
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
            this.setSlotRedirect(2, new PredicateLimitedSlot(PlacerBlockEntity.this, 0, s -> s.getItem() instanceof BlockItem || s.isIn(FactoryItemTags.PLACER_USABLE)));
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
