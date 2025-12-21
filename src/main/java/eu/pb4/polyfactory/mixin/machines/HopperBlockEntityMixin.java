package eu.pb4.polyfactory.mixin.machines;

import eu.pb4.polyfactory.block.other.CustomBlockEntityCalls;
import eu.pb4.polyfactory.block.other.FilteredBlockEntity;
import eu.pb4.polyfactory.models.HopperModel;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends RandomizableContainerBlockEntity implements FilteredBlockEntity, CustomBlockEntityCalls {
    @Unique
    private ItemStack filterStack = ItemStack.EMPTY;
    @Unique
    private FilterData filter = FilterData.EMPTY_TRUE;
    @Nullable
    @Unique
    private HopperModel model;

    protected HopperBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void writeFilterNbt(ValueOutput view, CallbackInfo ci) {
        if (!this.filterStack.isEmpty()) {
            view.store("polyfactory:filter", ItemStack.CODEC, this.filterStack);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void readFilterNbt(ValueInput view, CallbackInfo ci) {
        polyfactory$setFilter(view.read("polyfactory:filter", ItemStack.OPTIONAL_CODEC).orElse(view.read("polydex:filter", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY)));
    }

    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private static void cancelUnfiltered(Container from, Container to, ItemStack stack, Direction side, CallbackInfoReturnable<ItemStack> cir) {
        if (to instanceof FilteredBlockEntity filteredHopper) {
            if (!filteredHopper.polyfactory$matchesFilter(stack)) {
                cir.setReturnValue(stack);
            }
        }
    }

    @Inject(method = "canTakeItemFromContainer", at = @At("HEAD"), cancellable = true)
    private static void cancelUnfiltered2(Container hopperInventory, Container fromInventory, ItemStack stack, int slot, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (hopperInventory instanceof FilteredBlockEntity filteredHopper) {
            if (!filteredHopper.polyfactory$matchesFilter(stack)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "pushItemsTick", at = @At("HEAD"))
    private static void setupModel(Level world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, CallbackInfo ci) {
        var self = (HopperBlockEntityMixin) (Object) blockEntity;
        if (self.model == null && !self.filterStack.isEmpty()) {
            self.createModel();
        }
    }

    @Override
    public void polyfactory$markRemoved() {
        if (this.model != null) {
            this.model.destroy();
            this.model = null;
        }
    }

    @Override
    public void polyfactory$setCachedState(BlockState state) {
        if (this.model != null) {
            this.model.updateRotation(state);
        }
    }

    @Override
    public ItemStack polyfactory$getFilter() {
        return this.filterStack;
    }

    @Override
    public void polyfactory$setFilter(ItemStack stack) {
        this.filterStack = stack;
        this.filter = FilterData.of(stack, true);

        if (this.model == null && !this.filterStack.isEmpty()) {
            this.createModel();
        } else if (this.model != null && this.filterStack.isEmpty()) {
            this.model.destroy();
            this.model = null;
        } else if (this.model != null) {
            this.model.setFilter(this.filter);
            this.model.tick();
        }
        this.setChanged();
    }

    @Unique
    private void createModel() {
        if (!(this.level instanceof ServerLevel serverWorld)) {
            return;
        }
        var model = new HopperModel(this.getBlockState());
        model.setFilter(this.filter);
        ChunkAttachment.of(model, serverWorld, this.worldPosition);
        this.model = model;
    }

    @Override
    public boolean polyfactory$matchesFilter(ItemStack itemStack) {
        return filter.test(itemStack);
    }

}
