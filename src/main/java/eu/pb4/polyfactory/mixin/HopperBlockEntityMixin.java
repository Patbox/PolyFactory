package eu.pb4.polyfactory.mixin;

import eu.pb4.polyfactory.block.other.CustomBlockEntityCalls;
import eu.pb4.polyfactory.block.other.FilteredBlockEntity;
import eu.pb4.polyfactory.item.tool.FilterItem;
import eu.pb4.polyfactory.models.HopperModel;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.SoftOverride;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends LootableContainerBlockEntity implements FilteredBlockEntity, CustomBlockEntityCalls {
    @Unique
    private ItemStack polyfactory$filterStack = ItemStack.EMPTY;
    @Unique
    private FilterItem.Data polyfactory$filter = FilterItem.createData(ItemStack.EMPTY, true);
    @Nullable
    @Unique
    private HopperModel polyfactory$model;

    protected HopperBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void polyfactory$writeFilterNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.put("polydex:filter", this.polyfactory$filterStack.writeNbt(new NbtCompound()));
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void polyfactory$readFilterNbt(NbtCompound nbt, CallbackInfo ci) {
        polyfactory$setFilter(ItemStack.fromNbt(nbt.getCompound("polydex:filter")));
    }

    @Inject(method = "transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private static void polyfactory$cancelUnfiltered(Inventory from, Inventory to, ItemStack stack, Direction side, CallbackInfoReturnable<ItemStack> cir) {
        if (to instanceof FilteredBlockEntity filteredHopper) {
            if (!filteredHopper.polyfactory$matchesFilter(stack)) {
                cir.setReturnValue(stack);
            }
        }
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void polyfactory$setupModel(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, CallbackInfo ci) {
        var self = (HopperBlockEntityMixin) (Object) blockEntity;
        if (self.polyfactory$model == null && !self.polyfactory$filterStack.isEmpty()) {
            self.polyfactory$createModel();
        }
    }

    @Override
    public void polyfactory$markRemoved() {
        if (this.polyfactory$model != null) {
            this.polyfactory$model.destroy();
            this.polyfactory$model = null;
        }
    }

    @Override
    public void polyfactory$setCachedState(BlockState state) {
        if (this.polyfactory$model != null) {
            this.polyfactory$model.updateRotation(state);
        }
    }

    @Override
    public ItemStack polyfactory$getFilter() {
        return this.polyfactory$filterStack;
    }

    @Override
    public void polyfactory$setFilter(ItemStack stack) {
        this.polyfactory$filterStack = stack;
        this.polyfactory$filter = FilterItem.createData(stack, true);
        if (this.polyfactory$model == null) {
            this.polyfactory$createModel();
        } else if (this.polyfactory$filterStack.isEmpty()) {
            this.polyfactory$model.destroy();
            this.polyfactory$model = null;
        } else {
            this.polyfactory$model.setItem(this.polyfactory$filter.icon());
            this.polyfactory$model.tick();
        }
        this.markDirty();
    }

    private void polyfactory$createModel() {
        if (!(this.world instanceof ServerWorld serverWorld)) {
            return;
        }
        var model = new HopperModel(this.getCachedState());
        model.setItem(this.polyfactory$filter.icon());
        ChunkAttachment.of(model, serverWorld, this.pos);
        this.polyfactory$model = model;
    }

    @Override
    public boolean polyfactory$matchesFilter(ItemStack itemStack) {
        return polyfactory$filter.test(itemStack);
    }

}
