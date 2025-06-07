package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.util.SimpleColoredItem;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polyfactory.block.other.WirelessRedstoneBlock;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.util.ColoredItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.function.Consumer;

public class WirelessRedstoneTransmitterItem extends Item implements SimpleColoredItem, ColoredItem {
    public WirelessRedstoneTransmitterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        var x = user.getStackInHand(hand).get(FactoryDataComponents.REMOTE_KEYS);

        if (x != null && world instanceof ServerWorld serverWorld) {
            WirelessRedstoneBlock.send(serverWorld, user.getBlockPos(), 20, x.getFirst(), x.getSecond());
            user.playSoundToPlayer(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1.5f);
            user.playSoundToPlayer(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_OFF, SoundCategory.PLAYERS, 1f, 1.4f);
            return ActionResult.SUCCESS_SERVER;
        }

        return super.use(world, user, hand);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        var x = stack.get(FactoryDataComponents.REMOTE_KEYS);
        if (x != null) {
            tooltip.accept(x.getFirst().toHoverableText().copy().formatted(Formatting.GRAY));
            tooltip.accept(x.getSecond().toHoverableText().copy().formatted(Formatting.GRAY));
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
