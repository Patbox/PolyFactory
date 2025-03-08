package eu.pb4.polyfactory.block.other;

import eu.pb4.polyfactory.mixin.ItemEntityAccessor;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.SimpleEntityElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class BlueprintWorkbenchBlock extends WorkbenchBlock {

    public BlueprintWorkbenchBlock(Settings settings) {
        super(settings);
    }

    private static void clickForCrafting(ServerPlayerEntity player, BlockPos blockPos) {
        if (!player.isSneaking() && player.getServerWorld().getBlockEntity(blockPos) instanceof BlueprintWorkbenchBlockEntity be) {
            be.clickForCrafting(player);
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!player.isSneaking() && world.getBlockEntity(pos) instanceof BlueprintWorkbenchBlockEntity be) {
            be.openGui((ServerPlayerEntity) player);
            return ActionResult.SUCCESS;
        }

        return super.onUse(state, world, pos, player, hit);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BlueprintWorkbenchBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    public static class Model extends WorkbenchBlock.Model {
        private final SimpleEntityElement result;
        private final InteractionElement clickable;

        private Model(BlockState state) {
            super(state);

            this.result = new SimpleEntityElement(EntityType.ITEM);
            this.result.setOffset(new Vec3d(0, 0.5, 0));
            this.clickable = new InteractionElement(new VirtualElement.InteractionHandler() {
                @Override
                public void interact(ServerPlayerEntity player, Hand hand) {
                    BlueprintWorkbenchBlock.clickForCrafting(player, blockPos());
                }
            });
            clickable.setSize(0.4f, 0.55f);
            clickable.setOffset(new Vec3d(0, 0.5, 0));
        }

        @Override
        protected void setupElement(ItemDisplayElement element, int i) {
            super.setupElement(element, i);
            element.setScale(new Vector3f(4 / 16f, 4 / 16f, 0.005f));
            element.setTranslation(new Vector3f(element.getTranslation()).add(0, -1.8f / 16f / 16f, 0));
        }

        public void setResult(ItemStack stack) {
            this.removeElement(this.result);
            this.removeElement(this.clickable);
            if (!stack.isEmpty()) {
                this.result.getDataTracker().set(ItemEntityAccessor.getSTACK(), stack.copy());
                this.addElement(this.result);
                this.addElement(this.clickable);
            }
        }
    }
}
