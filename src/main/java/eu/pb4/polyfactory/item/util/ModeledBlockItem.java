package eu.pb4.polyfactory.item.util;

import eu.pb4.polyfactory.models.BaseItemProvider;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

public class ModeledBlockItem extends BlockItem implements SimpleModeledPolymerItem {

    private final Item polymerItem;

    public <T extends Block & PolymerBlock> ModeledBlockItem(T block, Settings settings) {
        super(block, settings);
        this.polymerItem = BaseItemProvider.requestSimpleItem();
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var x = super.useOnBlock(context);
        if (x == ActionResult.CONSUME) {
            if (context.getPlayer() instanceof ServerPlayerEntity player) {
                var pos = Vec3d.ofCenter(context.getBlockPos().offset(context.getSide()));
                var blockSoundGroup = this.getBlock().getSoundGroup(this.getBlock().getDefaultState());
                player.networkHandler.sendPacket(new PlaySoundS2CPacket(Registries.SOUND_EVENT.getEntry(this.getPlaceSound(this.getBlock().getDefaultState())), SoundCategory.BLOCKS, pos.x, pos.y, pos.z, (blockSoundGroup.getVolume() + 1.0F) / 2.0F, blockSoundGroup.getPitch() * 0.8F, context.getPlayer().getRandom().nextLong()));
            }
            return ActionResult.SUCCESS;
        }
        return x;
    }

    @Override
    public final Item getPolymerItem() {
        return this.polymerItem;
    }
}
