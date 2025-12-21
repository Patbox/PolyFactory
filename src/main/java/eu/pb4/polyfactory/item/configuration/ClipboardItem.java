package eu.pb4.polyfactory.item.configuration;

import com.mojang.serialization.JavaOps;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ClipboardItem extends SimplePolymerItem {
    public ClipboardItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        var data = user.getItemInHand(hand).get(FactoryDataComponents.CONFIGURATION_DATA);


        if (data != null && user.isSecondaryUseActive()) {
            user.getItemInHand(hand).set(FactoryDataComponents.CONFIGURATION_DATA, null);
            FactoryUtil.playSoundToPlayer(user, FactorySoundEvents.ITEM_CLIPBOARD_WRITE, SoundSource.PLAYERS, 1, 1);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.use(world, user, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getPlayer() instanceof ServerPlayer player) || (context.getPlayer() != null && !context.getPlayer().mayBuild())) {
            return InteractionResult.FAIL;
        }
        var state = context.getLevel().getBlockState(context.getClickedPos());
        //noinspection unchecked
        var blockConfig = state.getBlock() instanceof ConfigurableBlock configurableBlock
                ? (List<BlockConfig<Object>>) (Object) configurableBlock.getBlockConfiguration(player, context.getClickedPos(), context.getClickedFace(), state) : List.<BlockConfig<Object>>of();

        if (blockConfig.isEmpty()) {
            return InteractionResult.FAIL;
        }

        var data = context.getItemInHand().get(FactoryDataComponents.CONFIGURATION_DATA);


        if (data == null || player.isSecondaryUseActive()) {
            var entries = new ArrayList<ConfigurationData.Entry>();

            for (var config : blockConfig) {
                var val = config.value().getValue(context.getLevel(), context.getClickedPos(), context.getClickedFace(), state);
                entries.add(new ConfigurationData.Entry(
                        config.name(),
                        config.formatter().getDisplayValue(val, context.getLevel(), context.getClickedPos(), context.getClickedFace(), state),
                        config.id(),
                        config.codec().encodeStart(JavaOps.INSTANCE, val).getOrThrow()
                ));
            }

            context.getItemInHand().set(FactoryDataComponents.CONFIGURATION_DATA, new ConfigurationData(entries));
            FactoryUtil.playSoundToPlayer(player,FactorySoundEvents.ITEM_CLIPBOARD_WRITE, SoundSource.PLAYERS, 1, 1);
            return InteractionResult.SUCCESS_SERVER;
        } else {
            var success = false;
            for (var config : blockConfig) {
                for (var entry : data.entries()) {
                    if (!config.id().equals(entry.id())) {
                        continue;
                    }

                    var decoded = config.codec().decode(JavaOps.INSTANCE, entry.value());

                    if (decoded.isSuccess()) {
                        success |= config.value().setValue(decoded.getOrThrow().getFirst(), context.getLevel(), context.getClickedPos(), context.getClickedFace(), state);
                    }
                }
            }

            if (success) {
                FactoryUtil.playSoundToPlayer(player,FactorySoundEvents.ITEM_CLIPBOARD_APPLY, SoundSource.PLAYERS, 1, 1);
                return InteractionResult.SUCCESS_SERVER;
            } else  {
                return InteractionResult.FAIL;
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(FactoryDataComponents.CONFIGURATION_DATA);
    }

    @Override
    public boolean isPolymerBlockInteraction(BlockState state, ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel world, BlockHitResult blockHitResult, InteractionResult actionResult) {
        return true;
    }

    @Override
    public boolean isPolymerEntityInteraction(ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel world, Entity entity, InteractionResult actionResult) {
        return true;
    }
}
