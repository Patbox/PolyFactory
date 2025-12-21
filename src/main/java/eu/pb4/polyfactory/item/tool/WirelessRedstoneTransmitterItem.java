package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.SimpleColoredItem;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polyfactory.block.other.WirelessRedstoneBlock;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.util.ColoredItem;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class WirelessRedstoneTransmitterItem extends Item implements SimpleColoredItem, ColoredItem {
    public WirelessRedstoneTransmitterItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        var x = user.getItemInHand(hand).get(FactoryDataComponents.REMOTE_KEYS);

        if (x != null && world instanceof ServerLevel serverWorld) {
            WirelessRedstoneBlock.send(serverWorld, user.blockPosition(), 20, x.getFirst(), x.getSecond());
            FactoryUtil.playSoundToPlayer(user, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.PLAYERS, 1f, 1.5f);
            FactoryUtil.playSoundToPlayer(user, SoundEvents.WOODEN_BUTTON_CLICK_OFF, SoundSource.PLAYERS, 1f, 1.4f);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.use(world, user, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> tooltip, TooltipFlag type) {
        var x = stack.get(FactoryDataComponents.REMOTE_KEYS);
        if (x != null) {
            tooltip.accept(x.getFirst().getDisplayName().copy().withStyle(ChatFormatting.GRAY));
            tooltip.accept(x.getSecond().getDisplayName().copy().withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public int getDefaultColor() {
        return 0x888888;
    }

    @Override
    public int getItemColor(ItemStack stack) {
        return stack.getOrDefault(FactoryDataComponents.COLOR, 0x888888);
    }
}
