package eu.pb4.polyfactory.item;

import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.CableBlock;
import eu.pb4.polyfactory.item.util.ModeledBlockItem;
import eu.pb4.polyfactory.mixin.ItemUsageContextAccessor;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.models.LodItemDisplayElement;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class CableItem extends ModeledBlockItem {
    public <T extends Block & PolymerBlock> CableItem(Settings settings) {
        super(FactoryBlocks.CABLE, settings);
    }


    @Override
    public ActionResult place(ItemPlacementContext context) {
        if (context.getPlayer() == null || context.getPlayer().canModifyBlocks()) {
            var hitResult = ((ItemUsageContextAccessor) context).callGetHitResult();
            var worldState = context.getWorld().getBlockState(hitResult.getBlockPos());
            var face = CableBlock.FACING_PROPERTIES.get(context.getSide());
            if (worldState.isOf(FactoryBlocks.CABLE) && !worldState.get(face)) {
                context.getWorld().setBlockState(hitResult.getBlockPos(), worldState.with(face, true));
                return ActionResult.SUCCESS;
            }
            worldState = context.getWorld().getBlockState(context.getBlockPos());
            face = CableBlock.FACING_PROPERTIES.get(context.getSide().getOpposite());
            if (worldState.isOf(FactoryBlocks.CABLE) && !worldState.get(face)) {
                context.getWorld().setBlockState(context.getBlockPos(), worldState.with(face, true));
                return ActionResult.SUCCESS;
            }
        }
        return super.place(context);
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(ItemPlacementContext context) {
        return super.getPlacementState(context);
    }
}
