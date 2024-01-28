package eu.pb4.polyfactory.item.tool;

import eu.pb4.factorytools.api.item.ModeledItem;
import eu.pb4.polyfactory.block.other.WirelessRedstoneReceiverBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class WirelessRedstoneTransmitterItem extends ModeledItem {
    public WirelessRedstoneTransmitterItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world instanceof ServerWorld serverWorld) {
            var stack = user.getStackInHand(hand);
            var key1 = ItemStack.fromNbt(stack.getOrCreateNbt().getCompound("key1"));
            var key2 = ItemStack.fromNbt(stack.getOrCreateNbt().getCompound("key2"));
            WirelessRedstoneReceiverBlock.send(serverWorld, user.getBlockPos(), 40, key1, key2);
            return TypedActionResult.success(stack);
        }

        return super.use(world, user, hand);
    }
}
