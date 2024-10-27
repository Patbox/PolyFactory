package eu.pb4.polyfactory.mixin.machines;

import eu.pb4.polyfactory.block.other.CustomBlockEntityCalls;
import eu.pb4.polyfactory.block.other.FilteredBlockEntity;
import eu.pb4.polyfactory.models.HopperModel;
import eu.pb4.polyfactory.util.filter.FilterData;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends LootableContainerBlockEntity implements FilteredBlockEntity, CustomBlockEntityCalls {
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

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void writeFilterNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup, CallbackInfo ci) {
        nbt.put("polydex:filter", this.filterStack.toNbtAllowEmpty(registryLookup));
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void readFilterNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup, CallbackInfo ci) {
        polyfactory$setFilter(ItemStack.fromNbtOrEmpty(registryLookup, nbt.getCompound("polydex:filter")));
    }

    @Inject(method = "transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private static void cancelUnfiltered(Inventory from, Inventory to, ItemStack stack, Direction side, CallbackInfoReturnable<ItemStack> cir) {
        if (to instanceof FilteredBlockEntity filteredHopper) {
            if (!filteredHopper.polyfactory$matchesFilter(stack)) {
                cir.setReturnValue(stack);
            }
        }
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void setupModel(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, CallbackInfo ci) {
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
            this.model.setItem(this.filter.icon());
            this.model.tick();
        }
        this.markDirty();
    }

    private void createModel() {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }
        var model = new HopperModel(this.getCachedState());
        model.setItem(this.filter.icon());
        ChunkAttachment.of(model, serverWorld, this.pos);
        this.model = model;
    }

    @Override
    public boolean polyfactory$matchesFilter(ItemStack itemStack) {
        return filter.test(itemStack);
    }

}
