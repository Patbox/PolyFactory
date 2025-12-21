package eu.pb4.polyfactory.block.other;

import eu.pb4.polyfactory.mixin.ItemEntityAccessor;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.SimpleEntityElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class BlueprintWorkbenchBlock extends WorkbenchBlock {

    public BlueprintWorkbenchBlock(Properties settings) {
        super(settings);
    }

    private static void clickForCrafting(ServerPlayer player, BlockPos blockPos) {
        if (!player.isShiftKeyDown() && player.level().getBlockEntity(blockPos) instanceof BlueprintWorkbenchBlockEntity be) {
            be.clickForCrafting(player);
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && world.getBlockEntity(pos) instanceof BlueprintWorkbenchBlockEntity be) {
            be.openGui((ServerPlayer) player);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlueprintWorkbenchBlockEntity(pos, state);
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new eu.pb4.polyfactory.block.other.BlueprintWorkbenchBlock.Model(initialBlockState);
    }

    public static class Model extends WorkbenchBlock.Model {
        private final SimpleEntityElement result;
        private final InteractionElement clickable;

        private Model(BlockState state) {
            super(state);

            this.result = new SimpleEntityElement(EntityType.ITEM);
            this.result.setOffset(new Vec3(0, 0.5, 0));
            this.clickable = new InteractionElement(new VirtualElement.InteractionHandler() {
                @Override
                public void interact(ServerPlayer player, InteractionHand hand) {
                    BlueprintWorkbenchBlock.clickForCrafting(player, blockPos());
                }
            });
            clickable.setSize(0.4f, 0.55f);
            clickable.setOffset(new Vec3(0, 0.5, 0));
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
                this.result.getDataTracker().set(ItemEntityAccessor.getDATA_ITEM(), stack.copy());
                this.addElement(this.result);
                this.addElement(this.clickable);
            }
        }
    }
}
