package eu.pb4.polyfactory.block.collection;

import java.util.Collection;
import java.util.function.Consumer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface BlockCollectionView {
    void polyfactory$provideCollision(AABB box, Consumer<VoxelShape> consumer);
    void polyfactory$addCollision(BlockCollection collection);
    void polyfactory$removeCollision(BlockCollection collection);
}
