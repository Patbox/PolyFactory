package eu.pb4.polyfactory.block.mechanical.machines;

import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.FactoryPlayer;
import eu.pb4.polyfactory.util.inventory.SingleStackInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.OperatorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

public class MinerBlockEntity extends BlockEntity implements SingleStackInventory {
    private ItemStack currentTool = ItemStack.EMPTY;
    private BlockState targetState = Blocks.AIR.getDefaultState();
    protected GameProfile owner = null;
    protected FactoryPlayer player = null;
    protected double process = 0;
    private float stress = 0;
    private MinerBlock.Model model;

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.MINER, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.put("Tool", this.currentTool.writeNbt(new NbtCompound()));
        nbt.putDouble("Progress", this.process);
        nbt.put("BlockState", NbtHelper.fromBlockState(this.targetState));
        if (this.owner != null) {
            nbt.put("Owner", NbtHelper.writeGameProfile(new NbtCompound(), this.owner));
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.currentTool = ItemStack.fromNbt(nbt.getCompound("Tool"));
        this.process = nbt.getDouble("Progress");
        if (nbt.contains("Owner")) {
            this.owner = NbtHelper.toGameProfile(nbt.getCompound("Owner"));
        }
        this.targetState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("BlockState"));
    }

    @Override
    public ItemStack getStack() {
        return this.currentTool;
    }

    @Override
    public void setStack(ItemStack stack) {
        this.currentTool = stack;
        if (this.model != null) {
            this.model.setItem(stack);
        }
    }

    public FactoryPlayer getFakePlayer() {
        if (this.player == null) {
            this.player = new FactoryPlayer(StackReference.of(this, 0), (ServerWorld) this.world, this.pos, this.owner == null ? FactoryUtil.GENERIC_PROFILE : this.owner);
        }

        return this.player;
    }


    public void openGui(ServerPlayerEntity player) {
        new Gui(player);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        var self = (MinerBlockEntity) t;

        if (self.model == null) {
            self.model = (MinerBlock.Model) BlockBoundAttachment.get(world, pos).holder();
            self.model.setItem(self.currentTool);
        }

        var blockPos = pos.offset(state.get(MinerBlock.FACING));
        var stateFront = world.getBlockState(blockPos);

        if (stateFront != self.targetState) {
            self.process = 0;
            self.targetState = stateFront;
            world.setBlockBreakingInfo(self.getFakePlayer().getId(), blockPos, -1);
            return;
        }
        var player = self.getFakePlayer();

        if (self.currentTool.isEmpty() || !self.currentTool.getItem().canMine(stateFront, world, blockPos, player)) {
            self.stress = 0;
            return;
        }

        if (!CommonProtection.canBreakBlock(world, blockPos, self.owner == null ? FactoryUtil.GENERIC_PROFILE : self.owner,null)) {
            self.stress = 0;
            return;
        }

        var delta = stateFront.calcBlockBreakingDelta(player, self.world, blockPos);
        if (delta < 0) {
            delta = 0;
        }

        var speed = Math.abs(RotationUser.getRotation((ServerWorld) world, pos).speed()) * MathHelper.RADIANS_PER_DEGREE * 2.5f;

        if (stateFront.isAir() || stateFront.getOutlineShape(world, blockPos).isEmpty()) {
            self.stress = 0;
            self.model.rotate((float) speed);
            return;
        }
        self.stress = Math.min(0.2f / delta, player.canHarvest(stateFront) ? 20 : 99999);

        if (speed == 0) {
            return;
        }

        self.process += delta * speed;

        self.model.rotate((float) speed);

        world.setBlockBreakingInfo(player.getId(), blockPos, (int) (self.process * 10.0F));

        if (self.process >= 1) {
            self.process = 0;
            self.stress = 0;

            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (!(stateFront.getBlock() instanceof OperatorBlock) && !player.isBlockBreakingRestricted(world, blockPos, GameMode.SURVIVAL)) {
                stateFront.getBlock().onBreak(world, blockPos, stateFront, player);
                boolean bl = world.removeBlock(blockPos, false);
                if (bl) {
                    stateFront.getBlock().onBroken(world, blockPos, stateFront);
                }

                ItemStack itemStack2 = self.currentTool.copy();
                boolean bl2 = player.canHarvest(stateFront);
                self.currentTool.postMine(world, stateFront, blockPos, player);
                if (bl && bl2) {
                    stateFront.getBlock().afterBreak(world, player, blockPos, stateFront, blockEntity, itemStack2);
                }
            }
            world.updateComparators(pos, state.getBlock());
        }
    }

    public float getStress() {
        return this.stress;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.HOPPER, player, false);
            this.setTitle(GuiTextures.CENTER_SLOT_GENERIC.apply(MinerBlockEntity.this.getCachedState().getBlock().getName()));
            this.setSlotRedirect(2, new Slot(MinerBlockEntity.this, 0, 0, 0));
            this.open();
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(MinerBlockEntity.this.pos)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }
}
