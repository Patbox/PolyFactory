package eu.pb4.polyfactory.block.mechanical.machines;

import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.OwnedBlockEntity;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.factorytools.api.util.FactoryPlayer;
import eu.pb4.factorytools.api.util.LegacyNbtHelper;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.PredicateLimitedSlot;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.ItemThrower;
import eu.pb4.polyfactory.util.inventory.SingleStackContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class PlacerBlockEntity extends LockableBlockEntity implements SingleStackContainer, WorldlyContainer, OwnedBlockEntity {
    protected GameProfile owner = null;
    protected double process = 0;
    private ItemStack stack = ItemStack.EMPTY;
    private float stress = 0;
    private PlacerBlock.Model model;
    private FactoryPlayer player;
    private int reach = 1;

    public PlacerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PLACER, pos, state);
    }

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
        var self = (PlacerBlockEntity) t;

        if (self.model == null) {
            self.model = (PlacerBlock.Model) BlockBoundAttachment.get(world, pos).holder();
            self.model.setItem(self.stack);
        }


        var usable = self.stack.is(FactoryItemTags.PLACER_USABLE);

        if (self.stack.isEmpty() || (!(self.stack.getItem() instanceof BlockItem) && !usable)) {
            self.stress = 0;
            self.model.setItem(ItemStack.EMPTY);
            return;
        }

        BlockPos blockPos = pos;
        int reach = self.reach;
        while (reach-- > 0) {
            blockPos = blockPos.relative(state.getValue(PlacerBlock.FACING));
            if (!world.getBlockState(blockPos).isAir() || !world.getBlockState(blockPos.relative(state.getValue(PlacerBlock.FACING))).isAir()) {
                break;
            }
        }


        if (!CommonProtection.canPlaceBlock(world, blockPos, self.owner == null ? FactoryUtil.GENERIC_PROFILE : self.owner, null) ||
                !CommonProtection.canInteractBlock(world, blockPos, self.owner == null ? FactoryUtil.GENERIC_PROFILE : self.owner, null)) {
            self.stress = 0;
            return;
        }

        var dir = state.getValue(PlacerBlock.FACING);
        var speed = Math.abs(RotationUser.getRotation(world, pos).speed()) * Mth.DEG_TO_RAD * 2.5f;


        var entities = world.getEntitiesOfClass(Entity.class, new AABB(blockPos), Entity::isPickable);

        var blockHitResult = new BlockHitResult(Vec3.atCenterOf(blockPos).relative(dir, 0.5), dir.getOpposite(), blockPos, true);

        var context = usable
                ? new UseOnContext(world, self.getFakePlayer(), InteractionHand.MAIN_HAND, self.stack, blockHitResult)
                : new DirectionalPlaceContext(world, blockPos, state.getValue(PlacerBlock.FACING), self.stack, dir.getOpposite()) {
            @Override
            public boolean replacingClickedOnBlock() {
                return true;
            }
        };


        if (!entities.isEmpty() && context instanceof DirectionalPlaceContext ctx && !ctx.canPlace()) {
            self.model.setItem(ItemStack.EMPTY);
            self.stress = 0;
            return;
        } else {
            self.model.setItem(self.stack.copyWithCount(1));
        }
        self.stress = self.stack.getItem() instanceof BlockItem blockItem
                ? (float) Math.min(Math.max(8, Math.log(blockItem.getBlock().defaultDestroyTime()) / Math.log(1.1)), 18)
                : 12
        ;

        if (speed == 0) {
            return;
        }

        self.process += speed / 30;

        if (self.process >= 1) {
            self.process = 0;
            self.stress = 0;

            var thrower = new ItemThrower(world, pos, dir, dir.getAxis().getPlane());

            boolean skip = false;

            if (!entities.isEmpty()) {
                var p = self.getFakePlayer();
                if (dir == Direction.UP) {
                    p.setYRot(0);
                    p.setXRot(-90);
                } else if (dir == Direction.DOWN) {
                    p.setYRot(0);
                    p.setXRot(90);
                } else {
                    p.setYRot(dir.toYRot());
                    p.setXRot(0);
                }

                var vec = Vec3.atCenterOf(pos).relative(state.getValue(PlacerBlock.FACING), 0.51);
                p.setPosRaw(vec.x, vec.y, vec.z);

                for (var entity : entities) {
                    var interaction = p.interactOn(entity, InteractionHand.MAIN_HAND);
                    thrower.dropContentsWithoutTool(p.getInventory());

                    if (interaction.consumesAction()) {
                        skip = true;
                        break;
                    }
                }
            }

            if (!skip && self.stack.getItem() instanceof BlockItem blockItem && context instanceof DirectionalPlaceContext ctx) {
                blockItem.place(ctx);
                if (world.getBlockEntity(blockPos) instanceof OwnedBlockEntity ownedBlockEntity) {
                    ownedBlockEntity.setOwner(self.owner);
                }
            } else if (!skip) {
                var p = self.getFakePlayer();
                if (dir == Direction.UP) {
                    p.setYRot(0);
                    p.setXRot(-90);
                } else if (dir == Direction.DOWN) {
                    p.setYRot(0);
                    p.setXRot(90);
                } else {
                    p.setYRot(dir.toYRot());
                    p.setXRot(0);
                }

                var vec = Vec3.atCenterOf(pos).relative(state.getValue(PlacerBlock.FACING), 0.51);
                p.setPosRaw(vec.x, vec.y, vec.z);

                var interaction = p.gameMode.useItemOn(p, world, self.stack, InteractionHand.MAIN_HAND, blockHitResult);
                thrower.dropContentsWithoutTool(p.getInventory());
                if (!interaction.consumesAction()) {
                    p.gameMode.useItem(p, world, self.stack, InteractionHand.MAIN_HAND);
                    thrower.dropContentsWithoutTool(p.getInventory());
                }
            }

            if (self.owner != null && world.getPlayerByUUID(self.owner.id()) instanceof ServerPlayer serverPlayer) {
                TriggerCriterion.trigger(serverPlayer, FactoryTriggers.PLACER_PLACES);
            }
            self.setChanged();
        }
        self.model.rotate((float) self.process);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        if (!this.stack.isEmpty()) {
            view.store("stack", ItemStack.OPTIONAL_CODEC, this.stack);
        }
        view.putDouble("progress", this.process);
        if (this.owner != null) {
            view.store("owner", CompoundTag.CODEC, LegacyNbtHelper.writeGameProfile(new CompoundTag(), this.owner));
        }
        view.putInt("reach", this.reach);
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        this.stack = view.read("stack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.process = view.getDoubleOr("progress", 0);
        view.read("owner", CompoundTag.CODEC).ifPresent(x -> this.owner = LegacyNbtHelper.toGameProfile(x));

        this.reach = view.getIntOr("reach", 1);
        super.loadAdditional(view);
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
        this.setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    protected void createGui(ServerPlayer playerEntity) {
        new Gui(playerEntity);
    }

    public float getStress() {
        return this.stress;
    }

    public int reach() {
        return this.reach;
    }

    public void setReach(int reach) {
        this.reach = reach;
        this.setChanged();
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

    public FactoryPlayer getFakePlayer() {
        if (this.player == null) {
            var profile = this.owner == null ? FactoryUtil.GENERIC_PROFILE : this.owner;

            this.player = new FactoryPlayer(SlotAccess.of(this::getStack, this::setStack), (ServerLevel) this.level, this.worldPosition,
                    new GameProfile(profile.id(), "Placer (" + profile.name() + ")")) {
                @Override
                public double getEyeY() {
                    return getY();
                }
            };
            var vec = Vec3.atCenterOf(this.worldPosition).relative(this.getBlockState().getValue(PlacerBlock.FACING), 0.51);

            this.player.setPosRaw(vec.x, vec.y, vec.z);
        }

        return this.player;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayer player) {
            super(MenuType.HOPPER, player, false);
            this.setTitle(GuiTextures.CENTER_SLOT_GENERIC.apply(PlacerBlockEntity.this.getBlockState().getBlock().getName()));
            this.setSlotRedirect(2, new PredicateLimitedSlot(PlacerBlockEntity.this, 0, s -> s.getItem() instanceof BlockItem || s.is(FactoryItemTags.PLACER_USABLE)));
            this.open();
        }

        @Override
        public void onClose() {
            super.onClose();
        }

        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(PlacerBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }
}
