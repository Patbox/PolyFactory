package eu.pb4.polyfactory.block.mechanical.machines;

import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.OwnedBlockEntity;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.factorytools.api.util.FactoryPlayer;
import eu.pb4.factorytools.api.util.LegacyNbtHelper;
import eu.pb4.factorytools.api.util.VirtualDestroyStage;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.TagLimitedSlot;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.ItemThrower;
import eu.pb4.polyfactory.util.inventory.SingleStackContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

public class MinerBlockEntity extends LockableBlockEntity implements SingleStackContainer, WorldlyContainer, OwnedBlockEntity {
    private ItemStack currentTool = ItemStack.EMPTY;
    private BlockState targetState = Blocks.AIR.defaultBlockState();
    protected GameProfile owner = null;
    protected MinerPlayer player = null;
    protected double process = 0;
    private float stress = 0;
    private float lastAttackedTicks = 0;
    private MinerBlock.Model model;
    private float attackCooldownPerTick = 1;
    private int reach = 2;


    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.MINER, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        if (!this.currentTool.isEmpty()) {
            view.store("tool", ItemStack.OPTIONAL_CODEC, this.currentTool);
        }
        view.putDouble("progress", this.process);
        view.store("block_state", ExtraCodecs.NBT, NbtUtils.writeBlockState(this.targetState));
        if (this.owner != null) {
            view.store("owner", ExtraCodecs.NBT, LegacyNbtHelper.writeGameProfile(new CompoundTag(), this.owner));
        }
        view.putFloat("last_attacked_ticks", this.lastAttackedTicks);
        view.putInt("reach", this.reach);
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        this.currentTool = view.read("tool", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.process = view.getDoubleOr("progress", 0);
        view.read("owner", CompoundTag.CODEC).ifPresent((x) -> this.owner = LegacyNbtHelper.toGameProfile(x));

        this.targetState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, view.read("block_state", CompoundTag.CODEC).orElse(new CompoundTag()));
        this.lastAttackedTicks = view.getFloatOr("last_attacked_ticks", -1);
        this.reach = view.getIntOr("reach", 1);

        super.loadAdditional(view);
        this.updateAttackCooldownPerTick();
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
    public ItemStack getStack() {
        return this.currentTool;
    }

    @Override
    public void setStack(ItemStack stack) {
        this.currentTool = stack;
        this.lastAttackedTicks = 0;
        this.updateAttackCooldownPerTick();
        if (this.model != null) {
            this.model.setItem(stack);
        }
        this.setChanged();
    }

    private void updateAttackCooldownPerTick() {
        var attackSpeed = new AtomicReference<>(4d);
        var multiplier = new DoubleArrayList();
        var multiplier2 = new DoubleArrayList();

        this.getStack().forEachModifier(EquipmentSlot.MAINHAND, ((entityAttributeRegistryEntry, value) -> {
            if (entityAttributeRegistryEntry == Attributes.ATTACK_SPEED) {
                switch (value.operation()) {
                    case ADD_VALUE -> attackSpeed.updateAndGet(v -> v + value.amount());
                    case ADD_MULTIPLIED_BASE -> multiplier.add(value.amount());
                    case ADD_MULTIPLIED_TOTAL -> multiplier2.add(value.amount());
                }
            }
        }));

        var baseSpeed = attackSpeed.get();
        var out = baseSpeed;

        for (var val : multiplier) {
            out += baseSpeed * val;
        }

        for (var val : multiplier2) {
            out *= 1.0 + val;
        }

        this.attackCooldownPerTick = (float) (1.0 / out * 20.0);
    }

    public float getAttackCooldownProgress() {
        return Mth.clamp(((float) this.lastAttackedTicks + 0.5f) / this.attackCooldownPerTick, 0.0F, 1.0F);
    }


    public MinerPlayer getFakePlayer() {
        if (this.player == null) {
            var profile = this.owner == null ? FactoryUtil.GENERIC_PROFILE : this.owner;

            this.player = new MinerPlayer(SlotAccess.of(this::getStack, this::setStack), (ServerLevel) this.level, this.worldPosition,
                    new GameProfile(profile.id(), "Miner (" + profile.name() + ")"));
            this.player.setPosRaw(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5f, this.worldPosition.getZ() + 0.5f);
        }

        return this.player;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public int reach() {
        return this.reach;
    }

    public void setReach(int reach) {
        this.reach = reach;
        this.setChanged();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.currentTool.isEmpty() && stack.is(FactoryItemTags.ALLOWED_IN_MINER);
    }

    @Override
    protected void createGui(ServerPlayer playerEntity) {
        new Gui(playerEntity);
    }

    public static <T extends BlockEntity> void ticker(Level world, BlockPos pos, BlockState state, T t) {
        var self = (MinerBlockEntity) t;

        if (self.model == null) {
            self.model = (MinerBlock.Model) BlockBoundAttachment.get(world, pos).holder();
            self.model.setItem(self.currentTool);
        }


        BlockPos blockPos = pos;
        BlockState stateFront = Blocks.AIR.defaultBlockState();
        int reach = self.reach;
        var dir = state.getValue(MinerBlock.FACING);
        while (stateFront.isAir() && reach-- > 0) {
            blockPos = blockPos.relative(dir);
            stateFront = world.getBlockState(blockPos);
        }

        var centered = pos.getCenter();

        var entities = world.getEntitiesOfClass(Entity.class, AABB.encapsulatingFullBlocks(pos, blockPos), Entity::isPickable);
        entities.sort(Comparator.comparingDouble(x -> x.position().distanceToSqr(centered)));

        if (!entities.isEmpty()) {
            var speed = Math.abs(RotationUser.getRotation(world, pos).speed()) * Mth.DEG_TO_RAD * 3f;

            self.process = 0;
            world.destroyBlockProgress(self.getFakePlayer().getId(), blockPos, -1);
            VirtualDestroyStage.updateState(self.getFakePlayer(), blockPos, stateFront, -1);

            if (self.getAttackCooldownProgress() != 1) {
                self.lastAttackedTicks += speed;
                self.stress = 15;
                self.model.rotate((float) speed / 3);
                return;
            }
            var player = self.getFakePlayer();
            player.setLastAttackedTicks(9999999);

            var attr = player.getAttributes();
            var dmg = player.getAttributes().getInstance(Attributes.ATTACK_DAMAGE);
            if (dmg != null) {
                dmg.setBaseValue(0);
                dmg.removeModifiers();
                self.getStack().forEachModifier(EquipmentSlot.MAINHAND, (type, value) -> {
                    if (type == Attributes.ATTACK_DAMAGE) {
                        dmg.addTransientModifier(value);

                    }
                });
            }
            player.attack(entities.getFirst());
            self.lastAttackedTicks = 0;
            return;
        }

        if (stateFront != self.targetState) {
            self.process = 0;
            self.targetState = stateFront;
            world.destroyBlockProgress(self.getFakePlayer().getId(), blockPos, -1);
            VirtualDestroyStage.updateState(self.getFakePlayer(), blockPos, stateFront, -1);
            return;
        }

        var player = self.getFakePlayer();

        if (self.currentTool.isEmpty() || !self.currentTool.getItem().canDestroyBlock(self.currentTool, stateFront, world, blockPos, player)) {
            self.stress = 0;
            return;
        }

        if (!CommonProtection.canBreakBlock(world, blockPos, self.owner == null ? FactoryUtil.GENERIC_PROFILE : self.owner, null)) {
            self.stress = 0;
            return;
        }

        var delta = stateFront.getDestroyProgress(player, self.level, blockPos);
        if (delta < 0) {
            delta = 0;
        }

        var speed = Math.abs(RotationUser.getRotation((ServerLevel) world, pos).speed()) * Mth.DEG_TO_RAD * 2.5f;

        if (stateFront.isAir() || stateFront.getShape(world, blockPos).isEmpty()) {
            self.stress = 0;
            self.model.rotate((float) speed);
            return;
        }
        self.stress = Math.min(0.2f / delta, player.hasCorrectToolForDrops(stateFront) ? 20 : 99999);

        if (speed == 0) {
            return;
        }

        self.process += delta * speed;

        self.model.rotate((float) speed);

        var value = (int) (self.process * 10.0F);
        world.destroyBlockProgress(player.getId(), blockPos, value);
        VirtualDestroyStage.updateState(player, blockPos, stateFront, value);

        if (self.process >= 1) {
            self.process = 0;
            self.stress = 0;

            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (!(stateFront.getBlock() instanceof GameMasterBlock) && !player.blockActionRestricted(world, blockPos, GameType.SURVIVAL)) {
                stateFront.getBlock().playerWillDestroy(world, blockPos, stateFront, player);
                boolean bl = world.removeBlock(blockPos, false);
                if (bl) {
                    stateFront.getBlock().destroy(world, blockPos, stateFront);
                }

                ItemStack itemStack2 = self.currentTool.copy();
                boolean bl2 = player.hasCorrectToolForDrops(stateFront);
                self.currentTool.mineBlock(world, stateFront, blockPos, player);
                if (bl && bl2) {
                    stateFront.getBlock().playerDestroy(world, player, blockPos, stateFront, blockEntity, itemStack2);
                    if (self.owner != null && world.getPlayerByUUID(self.owner.id()) instanceof ServerPlayer serverPlayer) {
                        TriggerCriterion.trigger(serverPlayer, FactoryTriggers.MINER_MINES);
                    }
                }
            }
            if (!player.getInventory().isEmpty()) {
                var thrower = new ItemThrower(world, pos, dir, dir.getAxis().getPlane());
                thrower.dropContentsWithoutTool(player.getInventory());
            }
            self.setChanged();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.player != null) {
            VirtualDestroyStage.destroy(this.player);
        }
    }

    public float getStress() {
        return this.stress;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder componentMapBuilder) {
        super.collectImplicitComponents(componentMapBuilder);
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayer player) {
            super(MenuType.HOPPER, player, false);
            this.setTitle(GuiTextures.CENTER_SLOT_GENERIC.apply(MinerBlockEntity.this.getBlockState().getBlock().getName()));
            this.setSlotRedirect(2, new TagLimitedSlot(MinerBlockEntity.this, 0, FactoryItemTags.ALLOWED_IN_MINER));
            this.open();
        }

        @Override
        public void onClose() {
            super.onClose();
        }

        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(MinerBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }

    public static class MinerPlayer extends FactoryPlayer {
        public MinerPlayer(SlotAccess toolReference, ServerLevel world, BlockPos pos, GameProfile gameProfile) {
            super(toolReference, world, pos, gameProfile);
        }

        public void setLastAttackedTicks(int tick) {
            this.attackStrengthTicker = tick;
        }
    }
}
