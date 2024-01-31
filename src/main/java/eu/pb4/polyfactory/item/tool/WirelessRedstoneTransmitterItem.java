package eu.pb4.polyfactory.item.tool;

import eu.pb4.factorytools.api.item.ModeledItem;
import eu.pb4.polyfactory.block.other.WirelessRedstoneBlock;
import eu.pb4.polyfactory.item.util.ColoredItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WirelessRedstoneTransmitterItem extends ModeledItem implements DyeableItem {
    public WirelessRedstoneTransmitterItem(Settings settings) {
        super(Items.LEATHER_HORSE_ARMOR, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world instanceof ServerWorld serverWorld) {
            var stack = user.getStackInHand(hand);
            var key1 = ItemStack.fromNbt(stack.getOrCreateNbt().getCompound("key1"));
            var key2 = ItemStack.fromNbt(stack.getOrCreateNbt().getCompound("key2"));
            WirelessRedstoneBlock.send(serverWorld, user.getBlockPos(), 20, key1, key2);
            user.playSound(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, SoundCategory.MASTER, 1f, 1.5f);
            user.playSound(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_OFF, SoundCategory.MASTER, 1f, 1.4f);
            return TypedActionResult.success(stack);
        }

        return super.use(world, user, hand);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        var key1 = ItemStack.fromNbt(stack.getOrCreateNbt().getCompound("key1"));
        var key2 = ItemStack.fromNbt(stack.getOrCreateNbt().getCompound("key2"));
        tooltip.add(key1.toHoverableText().copy().formatted(Formatting.GRAY));
        tooltip.add(key2.toHoverableText().copy().formatted(Formatting.GRAY));
    }

    @Override
    public int getColor(ItemStack stack) {
        NbtCompound nbtCompound = stack.getSubNbt("display");
        return nbtCompound != null && nbtCompound.contains("color", NbtElement.NUMBER_TYPE)
                ? nbtCompound.getInt("color") : 0x888888;
    }

    @Override
    public int getPolymerArmorColor(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return getColor(itemStack);
    }
}
