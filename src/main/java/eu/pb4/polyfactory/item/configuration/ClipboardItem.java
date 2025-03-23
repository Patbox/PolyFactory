package eu.pb4.polyfactory.item.configuration;

import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ClipboardItem extends SimplePolymerItem {
    public ClipboardItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        var data = stack.get(FactoryDataComponents.CLIPBOARD_DATA);
        if (data != null) {
            data.addToTooltip(tooltip);
        }
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        var data = user.getStackInHand(hand).get(FactoryDataComponents.CLIPBOARD_DATA);


        if (data != null && user.shouldCancelInteraction()) {
            user.getStackInHand(hand).set(FactoryDataComponents.CLIPBOARD_DATA, null);
            user.playSoundToPlayer(FactorySoundEvents.ITEM_CLIPBOARD_WRITE, SoundCategory.PLAYERS, 1, 1);
            return ActionResult.SUCCESS_SERVER;
        }

        return super.use(world, user, hand);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getPlayer() instanceof ServerPlayerEntity player) || (context.getPlayer() != null && !context.getPlayer().canModifyBlocks())) {
            return ActionResult.FAIL;
        }
        var state = context.getWorld().getBlockState(context.getBlockPos());
        //noinspection unchecked
        var blockConfig = state.getBlock() instanceof ConfigurableBlock configurableBlock
                ? (List<BlockConfig<Object>>) (Object) configurableBlock.getBlockConfiguration(player, context.getBlockPos(), context.getSide(), state) : List.<BlockConfig<Object>>of();

        if (blockConfig.isEmpty()) {
            return ActionResult.FAIL;
        }

        var data = context.getStack().get(FactoryDataComponents.CLIPBOARD_DATA);


        if (data == null || player.shouldCancelInteraction()) {
            var entries = new ArrayList<ClipboardData.Entry>();

            for (var config : blockConfig) {
                var val = config.value().getValue(context.getWorld(), context.getBlockPos(), context.getSide(), state);
                entries.add(new ClipboardData.Entry(
                        config.name(),
                        config.formatter().getDisplayValue(val, context.getWorld(), context.getBlockPos(), context.getSide(), state),
                        config.id(),
                        config.codec().encodeStart(JavaOps.INSTANCE, val).getOrThrow()
                ));
            }

            context.getStack().set(FactoryDataComponents.CLIPBOARD_DATA, new ClipboardData(entries));
            player.playSoundToPlayer(FactorySoundEvents.ITEM_CLIPBOARD_WRITE, SoundCategory.PLAYERS, 1, 1);
            return ActionResult.SUCCESS_SERVER;
        } else {
            var success = false;
            for (var config : blockConfig) {
                for (var entry : data.entries()) {
                    if (!config.id().equals(entry.id())) {
                        continue;
                    }

                    var decoded = config.codec().decode(JavaOps.INSTANCE, entry.value());

                    if (decoded.isSuccess()) {
                        success |= config.value().setValue(decoded.getOrThrow().getFirst(), context.getWorld(), context.getBlockPos(), context.getSide(), state);
                    }
                }
            }

            if (success) {
                player.playSoundToPlayer(FactorySoundEvents.ITEM_CLIPBOARD_APPLY, SoundCategory.PLAYERS, 1, 1);
                return ActionResult.SUCCESS_SERVER;
            } else  {
                return ActionResult.FAIL;
            }
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return stack.contains(FactoryDataComponents.CLIPBOARD_DATA);
    }
}
