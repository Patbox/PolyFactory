package eu.pb4.polyfactory.item.tool;

import eu.pb4.factorytools.api.item.ModeledItem;
import eu.pb4.polyfactory.block.other.WirelessRedstoneBlock;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.util.ColoredItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
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

public class WirelessRedstoneTransmitterItem extends ModeledItem implements ColoredItem {
    public WirelessRedstoneTransmitterItem(Settings settings) {
        super(Items.LEATHER_HORSE_ARMOR, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var x = user.getStackInHand(hand).get(FactoryDataComponents.REMOTE_KEYS);

        if (x != null && world instanceof ServerWorld serverWorld) {
            WirelessRedstoneBlock.send(serverWorld, user.getBlockPos(), 20, x.getFirst(), x.getSecond());
            user.playSoundToPlayer(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, SoundCategory.MASTER, 1f, 1.5f);
            user.playSoundToPlayer(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_OFF, SoundCategory.MASTER, 1f, 1.4f);
            return TypedActionResult.success(user.getStackInHand(hand));
        }

        return super.use(world, user, hand);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        var x = stack.get(FactoryDataComponents.REMOTE_KEYS);
        if (x != null) {
            tooltip.add(x.getFirst().toHoverableText().copy().formatted(Formatting.GRAY));
            tooltip.add(x.getSecond().toHoverableText().copy().formatted(Formatting.GRAY));
        }
    }

    @Override
    public int getDefaultColor() {
        return 0x888888;
    }

    @Override
    public int getPolymerArmorColor(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return itemStack.getOrDefault(FactoryDataComponents.COLOR, 0x888888);
    }
}
