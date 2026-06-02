package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;

public class BrittleBottleItem extends Item implements PolymerItem {
    public BrittleBottleItem(final Item.Properties properties) {
        super(properties);
    }

    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, Fluid.SOURCE_ONLY);
        if (hitResult.getType() == Type.MISS) {
            return InteractionResult.PASS;
        } else {
            if (hitResult.getType() == Type.BLOCK) {
                BlockPos pos = hitResult.getBlockPos();
                if (!level.mayInteract(player, pos)) {
                    return InteractionResult.PASS;
                }

                if (level.getFluidState(pos).is(FluidTags.WATER)) {
                    level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
                    return InteractionResult.SUCCESS.heldItemTransformedTo(this.turnBottleIntoItem(itemStack, player, PotionContents.createItemStack(FactoryItems.BRITTLE_POTION, Potions.WATER)));
                }
            }

            return InteractionResult.PASS;
        }
    }

    protected ItemStack turnBottleIntoItem(final ItemStack itemStack, final Player player, final ItemStack itemStackToTurnInto) {
        player.awardStat(Stats.ITEM_USED.get(this));
        return ItemUtils.createFilledResult(itemStack, player, itemStackToTurnInto);
    }


    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TRIAL_KEY;
    }
}
