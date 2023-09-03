package eu.pb4.polyfactory.util;

import com.mojang.authlib.GameProfile;
import eu.pb4.polyfactory.mixin.player.PlayerEntityAccessor;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class FactoryPlayer extends FakePlayer {
    private final StackReference toolReference;

    public FactoryPlayer(StackReference toolReference, ServerWorld world, BlockPos pos, GameProfile gameProfile) {
        super(world, gameProfile);
        this.setPos(pos.getX(), pos.getY(), pos.getZ());
        this.toolReference = toolReference;
        ((PlayerEntityAccessor) this).setInventory(new FakeInventory(this));
    }

    @Override
    public ItemStack getStackInHand(Hand hand) {
        if (hand == Hand.MAIN_HAND) {
            return this.toolReference.get();
        }

        return super.getStackInHand(hand);
    }

    @Override
    public void setStackInHand(Hand hand, ItemStack stack) {
        if (hand == Hand.MAIN_HAND) {
            this.toolReference.set(stack);
            return;
        }

        super.setStackInHand(hand, stack);
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.toolReference.get();
        }

        return super.getEquippedStack(slot);
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            this.toolReference.set(stack);
            return;
        }

        super.equipStack(slot, stack);
    }

    @Override
    public boolean isSpectator() {
        return false;
    }

    @Override
    public boolean isCreative() {
        return false;
    }

    public class FakeInventory extends PlayerInventory {
        public FakeInventory(PlayerEntity player) {
            super(player);
        }

        @Override
        public ItemStack getMainHandStack() {
            return FactoryPlayer.this.toolReference.get();
        }

        @Override
        public void offer(ItemStack stack, boolean notifiesClient) {
            FactoryPlayer.this.getWorld().spawnEntity(new ItemEntity(FactoryPlayer.this.getWorld(), FactoryPlayer.this.getX(), FactoryPlayer.this.getY(), FactoryPlayer.this.getZ(), stack));
        }

        @Override
        public float getBlockBreakingSpeed(BlockState block) {
            return this.getMainHandStack().getMiningSpeedMultiplier(block);
        }
    }
}
