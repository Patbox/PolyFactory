package eu.pb4.polyfactory.mixin.block;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.polyfactory.block.collection.BlockCollectionView;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EntityView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(EntityView.class)
public interface EntityViewMixin {

    @WrapOperation(method = "getEntityCollisions", at = @At(value = "INVOKE", target = "Ljava/util/List;of()Ljava/util/List;", ordinal = 1))
    private List<VoxelShape> addBlockCollections(Operation<List<VoxelShape>> original, @Local(argsOnly = true) Box box) {
        if (this instanceof BlockCollectionView view) {
            var list = new ArrayList<VoxelShape>();
            view.polyfactory$provideCollision(box, list::add);
            list.addAll(original.call());
            return list;
        }

        return original.call();
    }

    @WrapOperation(method = "getEntityCollisions", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList$Builder;build()Lcom/google/common/collect/ImmutableList;"))
    private ImmutableList<VoxelShape> addBlockCollections(ImmutableList.Builder<VoxelShape> instance, Operation<ImmutableList<VoxelShape>> original,
                                                          @Local(argsOnly = true) Box box) {
        if (this instanceof BlockCollectionView view) {
            view.polyfactory$provideCollision(box, instance::add);
        }
        return original.call(instance);
    }
}
